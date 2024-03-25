package soot.jimple.infoflow.collections.solver.fastSolver;

import java.util.*;
import java.util.function.Consumer;

import heros.FlowFunction;
import heros.SynchronizedBy;
import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.collections.strategies.appending.AppendingStrategy;
import soot.jimple.infoflow.collections.util.ConcurrentSetWithRunnable;
import soot.jimple.infoflow.collections.util.MySpecialMultiMap;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.EndSummary;
import soot.jimple.infoflow.solver.IncomingRecord;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.fastSolver.InfoflowSolver;

/**
 * Infoflow Solver that supports various optimizations for precisely tracking collection keys/indices
 * - Supports widening on CallToReturn edges
 * - Tracks whether a ContextDefinition is affected by a method and tries to append similar abstractions to an
 *   already existing flow. If the flow notices that this was wrong, it reinjects the similar abstractions in
 *   the callee.
 *
 * @author Tim Lange
 */
public class AppendingCollectionInfoflowSolver extends InfoflowSolver {
    private AppendingStrategy<Unit, Abstraction> appending;

    public void setAppendingStrategy(AppendingStrategy<Unit, Abstraction> appending) {
        this.appending = appending;
    }

    public AppendingStrategy<Unit, Abstraction> getAppendingStrategy() {
        return this.appending;
    }

    // Map (Method, Param) to whether it may reach a context dependent operation. Contains == not reusable.
    @SynchronizedBy("Thread-safe data structure")
    private final ConcurrentSetWithRunnable<Pair<SootMethod, Local>> notReusable = new ConcurrentSetWithRunnable<>();

    // Holds the first abstraction that reached the callee, removing the context for the key. Similar abstractions can
    // append to this abstraction to reuse facts from this (unless the IFDS solver noticed the context is relevant).
    @SynchronizedBy("Thread-safe data structure")
    protected final MySpecialMultiMap<NoContextKey, IncomingRecord<Unit, Abstraction>> similarAbstractions = new MySpecialMultiMap<>();

    @Override
    public boolean addIncoming(SootMethod m, Abstraction d3, Unit n, Abstraction d1, Abstraction d2) {
        // Prohibit usage to prevent bugs
        throw new RuntimeException("Use addIncoming(SootMethod, Abstraction, Unit, Abstraction, Abstraction, Abstraction) instead!");
    }

    /**
     * Add a incoming record to the incoming set. If d3a == d3, then this behaves as addIncoming of an unmodified
     * IFDS solver. If d3a != d3, d3 is appended to d3a and reuses summaries of that abstraction unless d3a is later
     * discovered to be context-dependent.
     *
     * @param m   method
     * @param d3a the calling context the record should be added to
     * @param n   call site
     * @param d1  calling context in the caller
     * @param d2  incoming abstraction at the call site
     * @param d3  calling context in the caller
     * @return    true if the record was freshly added
     */
    protected boolean addIncoming(SootMethod m, Abstraction d3a, Unit n, Abstraction d1, Abstraction d2, Abstraction d3) {
        return peerGroup.addIncoming(m, n, d3a, d1, d2, d3);
    }

    public AppendingCollectionInfoflowSolver(AbstractInfoflowProblem problem, InterruptableExecutor executor) {
        super(problem, executor);
    }

    protected void processCall(PathEdge<Unit, Abstraction> edge) {
        final Abstraction d1 = edge.factAtSource();
        final Unit n = edge.getTarget(); // a call node; line 14...
        final Abstraction d2 = edge.factAtTarget();
        assert d2 != null;
        Collection<Unit> returnSiteNs = icfg.getReturnSitesOfCallAt(n);

        // for each possible callee
        Collection<SootMethod> callees = icfg.getCalleesOfCallAt(n);
        if (callees != null && !callees.isEmpty()) {
            if (maxCalleesPerCallSite < 0 || callees.size() <= maxCalleesPerCallSite) {
                callees.stream().filter(m -> m.isConcrete()).forEach(new Consumer<SootMethod>() {

                    @Override
                    public void accept(SootMethod sCalledProcN) {
                        // Early termination check
                        if (killFlag != null)
                            return;

                        // compute the call-flow function
                        FlowFunction<Abstraction> function = flowFunctions.getCallFlowFunction(n, sCalledProcN);
                        Set<Abstraction> res = computeCallFlowFunction(function, d1, d2);

                        // nothing to do if no facts flows into the callee
                        if (res == null || res.isEmpty())
                            return;

                        Collection<Unit> startPointsOf = icfg.getStartPointsOf(sCalledProcN);
                        // for each result node of the call-flow function
                        for (Abstraction d3 : res) {
                            if (memoryManager != null)
                                d3 = memoryManager.handleGeneratedMemoryObject(d2, d3);
                            if (d3 == null)
                                continue;

                            // We can only reuse summaries that have a non-zero context
                            if (appending.hasContext(d3)
                                    && !d3.getAccessPath().isStaticFieldRef()) {
                                // Check whether we have a similar abstraction that is or was already propagated through
                                // the callee. This also atomically adds the given record to the incoming set if there
                                // is a fitting abstraction, preventing races in the reinject method.
                                Abstraction prevSeenAbs = isReusable(sCalledProcN, n, d1, d2, d3);
                                if (prevSeenAbs != null) {
                                    // Do not propagate the abstraction in the caller but rather hope that
                                    // the similar abstraction can be reused in its summary
                                    applyEndSummaryOnCallWith(d1, n, d2, returnSiteNs, sCalledProcN, d3, prevSeenAbs);

                                    // We are done here
                                    continue;                                }
                            }

                            // register the fact that <sp,d3> has an incoming edge from
                            // <n,d2>
                            // line 15.1 of Naeem/Lhotak/Rodriguez
                            if (!addIncoming(sCalledProcN, d3, n, d1, d2, d3))
                                continue;

                            if (applyEndSummaryOnCallWith(d1, n, d2, returnSiteNs, sCalledProcN, d3, d3))
                                continue;

                            for (Unit sP : startPointsOf) {
                                // create initial self-loop
                                schedulingStrategy.propagateCallFlow(d3, sP, d3, n, false); // line 15
                            }
                        }
                    }
                });
            }
        }

        // line 17-19 of Naeem/Lhotak/Rodriguez
        // process intra-procedural flows along call-to-return flow functions
        for (Unit returnSiteN : returnSiteNs) {
            FlowFunction<Abstraction> callToReturnFlowFunction = flowFunctions.getCallToReturnFlowFunction(n, returnSiteN);
            Set<Abstraction> res = computeCallToReturnFlowFunction(callToReturnFlowFunction, d1, d2);
            if (res != null && !res.isEmpty()) {
                for (Abstraction d3 : res) {
                    if (memoryManager != null)
                        d3 = memoryManager.handleGeneratedMemoryObject(d2, d3);
                    if (d3 != null) {
                        schedulingStrategy.propagateCallToReturnFlow(d1, returnSiteN, d3, n, true);
                    }
                }
            }
        }
    }

    @Override
    public void applySummary(SootMethod callee, Abstraction d3a, Unit callSite,
                             Abstraction d2, Abstraction d1, Abstraction d3) {
        Collection<Unit> returnSiteNs = icfg.getReturnSitesOfCallAt(callSite);
        applyEndSummaryOnCallWith(d1, callSite, d2, returnSiteNs, callee, d3, d3a);
    }

    /**
     * Same as processExit() but with diffing of similar abstractions
     *
     * @param edge return edge
     */
    private void processExitIFDSSolver(PathEdge<Unit, Abstraction> edge) {
        final Unit n = edge.getTarget(); // an exit node; line 21...
        SootMethod methodThatNeedsSummary = icfg.getMethodOf(n);

        final Abstraction d1 = edge.factAtSource();
        final Abstraction d2 = edge.factAtTarget();

        // for each of the method's start points, determine incoming calls

        // line 21.1 of Naeem/Lhotak/Rodriguez
        // register end-summary
        if (!addEndSummary(methodThatNeedsSummary, d1, n, d2))
            return;

        Set<IncomingRecord<Unit, Abstraction>> inc = incoming(d1, methodThatNeedsSummary);
        // for each incoming call edge already processed
        // (see processCall(..))
        for (IncomingRecord<Unit, Abstraction> record : inc) {
            // Early termination check
            if (killFlag != null)
                return;

            // line 22
            Unit c = record.n;
            Set<Abstraction> callerSideDs = Collections.singleton(record.d1);

            final Abstraction d4 = record.d1;
            final Abstraction predVal = record.d2;

            // for each return site
            for (Unit retSiteC : icfg.getReturnSitesOfCallAt(c)) {
                // compute return-flow function
                FlowFunction<Abstraction> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary, n,
                        retSiteC);

                // for each incoming-call value
                Set<Abstraction> targets = computeReturnFlowFunction(retFunction, d1, d2, c, callerSideDs);
                if (targets != null && !targets.isEmpty()) {
                    for (Abstraction d5 : targets) {
                        if (memoryManager != null)
                            d5 = memoryManager.handleGeneratedMemoryObject(d2, d5);
                        if (d5 == null)
                            continue;

                        // If we have not changed anything in the callee, we do not need the facts from
                        // there. Even if we change something: If we don't need the concrete path, we
                        // can skip the callee in the predecessor chain
                        Abstraction d5p = shortenPredecessors(d5, predVal, d1, n, c);
                        schedulingStrategy.propagateReturnFlow(d4, retSiteC, d5p, c, false);
                    }
                }
            }
        }

        similarAbstractions.consumeOtherValues(new NoContextKey(methodThatNeedsSummary, d1),
                record -> {
                    Abstraction similarD4 = record.d1;
                    Abstraction similarPredVal = record.d2;
                    Abstraction similarD1 = record.d3;
                    Abstraction similarD2 = appending.applyDiffOf(d1, d2, similarD1);
                    // Early termination check
                    if (killFlag != null)
                        return;

                    // line 22
                    Unit c = record.n;
                    Set<Abstraction> callerSideDs = Collections.singleton(similarD4);

                    // for each return site
                    for (Unit retSiteC : icfg.getReturnSitesOfCallAt(c)) {
                        // compute return-flow function
                        FlowFunction<Abstraction> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary, n,
                                retSiteC);

                        // for each incoming-call value
                        Set<Abstraction> targets = computeReturnFlowFunction(retFunction, similarD1, similarD2, c, callerSideDs);
                        if (targets != null && !targets.isEmpty()) {
                            for (Abstraction d5 : targets) {
                                if (memoryManager != null)
                                    d5 = memoryManager.handleGeneratedMemoryObject(similarD2, d5);
                                if (d5 == null)
                                    continue;

                                // If we have not changed anything in the callee, we do not need the facts from
                                // there. Even if we change something: If we don't need the concrete path, we
                                // can skip the callee in the predecessor chain
                                Abstraction d5p = shortenPredecessors(d5, similarPredVal, similarD1, n, c);
                                schedulingStrategy.propagateReturnFlow(similarD4, retSiteC, d5p, c, false);
                            }
                        }
                    }
                });

        // handling for unbalanced problems where we return out of a method with
        // a fact for which we have no incoming flow
        // note: we propagate that way only values that originate from ZERO, as
        // conditionally generated values should only be propagated into callers that
        // have an incoming edge for this condition
        if (followReturnsPastSeeds && d1 == zeroValue && inc.isEmpty()) {
            Collection<Unit> callers = icfg.getCallersOf(methodThatNeedsSummary);
            for (Unit c : callers) {
                for (Unit retSiteC : icfg.getReturnSitesOfCallAt(c)) {
                    FlowFunction<Abstraction> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary, n,
                            retSiteC);
                    Set<Abstraction> targets = computeReturnFlowFunction(retFunction, d1, d2, c,
                            Collections.singleton(zeroValue));
                    if (targets != null && !targets.isEmpty()) {
                        for (Abstraction d5 : targets) {
                            if (memoryManager != null)
                                d5 = memoryManager.handleGeneratedMemoryObject(d2, d5);
                            if (d5 != null)
                                schedulingStrategy.propagateReturnFlow(zeroValue, retSiteC, d5, c, true);
                        }
                    }
                }
            }
            // in cases where there are no callers, the return statement would
            // normally not be processed at all; this might be undesirable if the flow
            // function has a side effect such as registering a taint; instead we thus call
            // the return flow function will a null caller
            if (callers.isEmpty()) {
                FlowFunction<Abstraction> retFunction = flowFunctions.getReturnFlowFunction(null, methodThatNeedsSummary, n,
                        null);
                retFunction.computeTargets(d2);
            }
        }
    }

    @Override
    protected void processExit(PathEdge<Unit, Abstraction> edge) {
        processExitIFDSSolver(edge);

        if (followReturnsPastSeeds && followReturnsPastSeedsHandler != null) {
            final Abstraction d1 = edge.factAtSource();
            final Unit u = edge.getTarget();
            final Abstraction d2 = edge.factAtTarget();

            final SootMethod methodThatNeedsSummary = icfg.getMethodOf(u);
            // Only change here to use our own incoming data structure
            if (incoming(d1, methodThatNeedsSummary).isEmpty())
                followReturnsPastSeedsHandler.handleFollowReturnsPastSeeds(d1, u, d2);
        }
    }

    @Override
    protected void applyEndSummaryOnCall(final Abstraction d1, final Unit n, final Abstraction d2, Collection<Unit> returnSiteNs,
                                             SootMethod sCalledProcN, Abstraction d3) {
        throw new RuntimeException("Use applyEndSummaryOnCallWith instead");
    }

    /**
     * Applying end summary but with the possibility to use another abstraction as a query for the end summaries.
     */
    protected boolean applyEndSummaryOnCallWith(final Abstraction d1, final Unit n, final Abstraction d2, Collection<Unit> returnSiteNs,
                                     SootMethod sCalledProcN, Abstraction d3, Abstraction summaryQuery) {
        // line 15.2
        Set<EndSummary<Unit, Abstraction>> endSumm = endSummary(sCalledProcN, summaryQuery);

        // This method is always called with d3 == summaryQuery unless we reuse summaries from similar
        // abstractions. Thus, referential equality is enough to deduce summary reuse
        boolean reuseOfSummary = d3 != summaryQuery;

        // We might have a race, where one path is context-insensitive and reached the end while the context-sensitive
        // path didn't reach the context-sensitive statement. But such a case won't change the outcome (and in the ideal
        // case, we actually would use this summary always but our analysis is not path sensitive, thus, we cant).
        // So we intentionally skip a check here whether the summary is reusable or not.

        // still line 15.2 of Naeem/Lhotak/Rodriguez
        // for each already-queried exit value <eP,d4> reachable
        // from <sP,d3>, create new caller-side jump functions to
        // the return sites because we have observed a potentially
        // new incoming edge into <sP,d3>
        if (endSumm != null && !endSumm.isEmpty()) {
            for (EndSummary<Unit, Abstraction> entry : endSumm) {
                Unit eP = entry.eP;
                Abstraction d4 = entry.d4;

                // If we reuse the summary, we have to fix up the incoming abstraction at the exit
                // to represent our initial context again.
                // Note that reusing the other path here without setting them as neighbors loses some
                // elements in the abstraction chain and, thus, might produce shorter paths.
                if (reuseOfSummary) {
                    d4 = appending.applyDiffOf(summaryQuery, d4, d3);
                } else {
                    // We must acknowledge the incoming abstraction from the other path
                    entry.calleeD1.addNeighbor(d3);
                }

                // for each return site
                for (Unit retSiteN : returnSiteNs) {
                    // compute return-flow function
                    FlowFunction<Abstraction> retFunction = flowFunctions.getReturnFlowFunction(n, sCalledProcN, eP, retSiteN);
                    Set<Abstraction> retFlowRes = computeReturnFlowFunction(retFunction, d3, d4, n, Collections.singleton(d1));
                    if (retFlowRes != null && !retFlowRes.isEmpty()) {
                        // for each target value of the function
                        for (Abstraction d5 : retFlowRes) {
                            if (memoryManager != null)
                                d5 = memoryManager.handleGeneratedMemoryObject(d4, d5);

                            // If we have not changed anything in
                            // the callee, we do not need the facts from
                            // there. Even if we change something:
                            // If we don't need the concrete path,
                            // we can skip the callee in the predecessor
                            // chain
                            Abstraction d5p = shortenPredecessors(d5, d2, d3, eP, n);
                            schedulingStrategy.propagateReturnFlow(d1, retSiteN, d5p, n, false);
                        }
                    }
                }
            }
            onEndSummaryApplied(n, sCalledProcN, d3);
            return true;
        }

        return false;
    }

    @Override
    protected void propagate(Abstraction sourceVal, Unit target, Abstraction targetVal,
            /* deliberately exposed to clients */ Unit relatedCallSite,
            /* deliberately exposed to clients */ boolean isUnbalancedReturn, ScheduleTarget scheduleTarget) {
        // Mark (method, param) as not reusable if needed
        if (sourceVal != zeroValue && appending.hasContext(targetVal) && appending.affectsContext(target)) {
            SootMethod sm = icfg.getMethodOf(target);
            if (isReusable(sm, sourceVal))
                reinject(sm, sourceVal);
        }

        super.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn, scheduleTarget);
    }

    /**
     * Mark (method, param) as not reusable
     * @param sm  method
     * @param abs abstraction
     */
    protected void markNotReusable(SootMethod sm, Abstraction abs) {
        notReusable.add(new Pair<>(sm, abs.getAccessPath().getPlainValue()));
    }

    /**
     * Checks whether (method, param) pair is reusable
     *
     * @param sm  method
     * @param abs abstraction
     * @return true if reusable
     */
    protected boolean isReusable(SootMethod sm, Abstraction abs) {
        return !notReusable.contains(new Pair<>(sm, abs.getAccessPath().getPlainValue()));
    }

    /**
     * Checks whether (method, param) is reusable and if so, adds the record to the given abstraction
     * while keeping the lock on the (method, param) pair.
     *
     * @param m   method
     * @param n   call site
     * @param d1  calling context at the call site
     * @param d2  incoming abstraction at the call site
     * @param d3  calling context in the callee
     * @return similar abstraction that was already propagated, or null if it is the first seen abstraction
     */
    protected Abstraction isReusable(SootMethod m, Unit n, Abstraction d1, Abstraction d2, Abstraction d3) {
        Abstraction[] runnableReturn = new Abstraction[1];
        // It is important to synchronize here such that isReusable cannot be flipped
        // before the abstraction is added to the incoming set. Otherwise, this might
        // race with the reinject method where the method is first marked reusable and
        // then walks up the incoming set to reinject possibly appended abstractions.
        boolean isReusable = notReusable.runIfAbsent(new Pair<>(m, d3.getAccessPath().getPlainValue()),
                                () -> {
                                    // We first cache incoming abstractions with contexts at call sites and maybe
                                    // get a already seen abstraction back
                                    IncomingRecord<Unit, Abstraction> rec = similarAbstractions.putAndGetFirst(new NoContextKey(m, d3),
                                            new IncomingRecord<>(n, d1, d2, d3));

                                    // If we have a cache hit, we want to use this abstraction to append. Though,
                                    // equal abstractions are already handled (better) by IFDS itself, so we skip
                                    // records where the callee abstraction is equal
                                    if (rec != null && !rec.d3.equals(d3))
                                        runnableReturn[0] = rec.d3;
                                });
        if (isReusable) {
            // This might be null if its the first seen abstraction
            return runnableReturn[0];
        }

        // We might encounter a callee, we already know is not reusable, on another
        // path. That means this path hasn't seen the context-dependent operation yet.
        // See testReinjectOnAlreadySeenCallee1.
        assert runnableReturn[0] == null;
        if (d1 != zeroValue)
            reinject(icfg.getMethodOf(n), d1);
        return null;
    }

    /**
     * Reinject all abstractions appended to the given context and its super contexts
     *
     * @param sm      current method
     * @param context calling context
     */
    protected void reinject(SootMethod sm, Abstraction context) {
        // We require a sensible context
        assert context != zeroValue;

        Deque<Pair<SootMethod, Abstraction>> toBeVisited = new ArrayDeque<>();
        // We assume the caller already knows that reinjecting is needed
        toBeVisited.add(new Pair<>(sm, context));
        while (!toBeVisited.isEmpty()) {
            Pair<SootMethod, Abstraction> p = toBeVisited.poll();
            Abstraction currAbs = p.getO2();
            SootMethod currMethod = p.getO1();

            // First step: mark method as not reusable
            markNotReusable(currMethod, currAbs);

            // Second step: reinject all appended abstractions at their appending point
            similarAbstractions.consumeOtherValuesAndRemove(new NoContextKey(currMethod, currAbs), record -> {
                final Abstraction d1 = record.d1;
                final Abstraction d2 = record.d2;
                final Abstraction d3 = record.d3;
                final Unit u = record.n;

                // Reinject the similar abstractions at the method start
                if (!addIncoming(currMethod, d3, u, d1, d2, d3))
                    return;

                if (applyEndSummaryOnCallWith(d1, u, d2, icfg.getReturnSitesOfCallAt(u), currMethod, d3, d3))
                    return;

                for (Unit sP : icfg.getStartPointsOf(currMethod)) {
                    // create initial self-loop
                    schedulingStrategy.propagateCallFlow(d3, sP, d3, u, false); // line 15
                }
            });

            // Third step: go up the call tree
            for (IncomingRecord<Unit, Abstraction> record : incoming(currAbs, currMethod)) {
                final Abstraction d1 = record.d1;
                final Unit u = record.n;

                SootMethod caller = icfg.getMethodOf(u);
                // We do not want to reinject everytime we see a context-dependent operation, because
                // reinjecting can be quite expensive in long call chains. We only reinject if the current
                // method is still marked as reusable.
                if (d1 != zeroValue && isReusable(caller, d1))
                    toBeVisited.add(new Pair<>(caller, d1));
            }
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.notReusable.clear();
        this.similarAbstractions.clear();
    }
}
