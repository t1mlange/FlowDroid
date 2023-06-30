package soot.jimple.infoflow.collections.solver.fastSolver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.sun.jdi.connect.Connector;
import heros.FlowFunction;
import heros.SynchronizedBy;
import heros.solver.Pair;
import heros.solver.PathEdge;
import org.checkerframework.checker.units.qual.A;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.EndSummary;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.util.ConcurrentHashMultiMap;
import soot.util.MultiMap;

/**
 * Infoflow Solver that supports various optimizations for precisely tracking collection keys/indices
 * - Supports widening on CallToReturn edges
 * - Tracks whether a ContextDefinition is affected by a method and tries to append similar abstractions to an
 *   already existing flow. If the flow notices that this was wrong, it reinjects the similar abstractions in
 *   the callee.
 *
 * @author Tim Lange
 */
public class AbstractingCollectionInfoflowSolver extends CollectionInfoflowSolver {
    // Map (Method, Param) to whether it may reach a context dependent operation
    private final ConcurrentHashSet<Pair<SootMethod, Local>> notReusable = new ConcurrentHashSet<>();

    // We need to overwrite the default incoming, because we might have multiple elements per context
    // e.g. when we add a concrete collection taint to use the summary of the abstracted collection taint
    @SynchronizedBy("Thread-safe data structure")
    protected final MyConcurrentHashMap<Pair<SootMethod, Abstraction>,
            MyConcurrentHashMap<Unit, MultiMap<Abstraction, Pair<Abstraction, Abstraction>>>> myIncoming = new MyConcurrentHashMap<>();

    // Holds the first abstraction that reached the callee, removing the context for the key. Similar abstractions can
    // append to this abstraction to reuse facts from this (unless the IFDS solver noticed the context is relevant).
    @SynchronizedBy("Thread-safe data structure")
    protected final Map<NoContextKey, Abstraction> incomingWContext = new ConcurrentHashMap<>();

    protected final MultiMap<Abstraction, Pair<SootMethod, Abstraction>> prevContext = new ConcurrentHashMultiMap<>();

    @Override
    protected boolean addIncoming(SootMethod m, Abstraction d3, Unit n, Abstraction d1, Abstraction d2) {
        throw new RuntimeException("Use addIncoming(SootMethod, Abstraction, Unit, Abstraction, Abstraction, Abstraction) instead!");
    }

    @Override
    protected Map<Unit, Map<Abstraction, Abstraction>> incoming(Abstraction d1, SootMethod m) {
        throw new RuntimeException("Use myIncoming(Abstraction, SootMethod) instead!");
    }

    protected boolean addIncoming(SootMethod m, Abstraction d3a, Unit n, Abstraction d1, Abstraction d2, Abstraction d3) {
        MyConcurrentHashMap<Unit, MultiMap<Abstraction, Pair<Abstraction, Abstraction>>> summaries
                = myIncoming.putIfAbsentElseGet(new Pair<>(m, d3a), MyConcurrentHashMap::new);
        MultiMap<Abstraction, Pair<Abstraction, Abstraction>> set = summaries.putIfAbsentElseGet(n, ConcurrentHashMultiMap::new);
        return set.put(d1, new Pair<>(d2, d3));
    }

    protected boolean removeAndAddIncoming(SootMethod m, Abstraction d3a, Unit n, Abstraction d1, Abstraction d2, Abstraction d3) {
        MyConcurrentHashMap<Unit, MultiMap<Abstraction, Pair<Abstraction, Abstraction>>> reuseSummaries
                = myIncoming.putIfAbsentElseGet(new Pair<>(m, d3a), MyConcurrentHashMap::new);
        MultiMap<Abstraction, Pair<Abstraction, Abstraction>> reuseSet = reuseSummaries.get(n);
        Pair<Abstraction, Abstraction> p = new Pair<>(d2, d3);

        return reuseSet.remove(d1, p) && addIncoming(m, d3, n, d1, d2, d3);
    }

    protected boolean incomingContains(SootMethod m, Abstraction d3a, Unit n, Abstraction d1, Abstraction d2, Abstraction d3) {
        MyConcurrentHashMap<Unit, MultiMap<Abstraction, Pair<Abstraction, Abstraction>>> reuseSummaries
                = myIncoming.putIfAbsentElseGet(new Pair<>(m, d3a), MyConcurrentHashMap::new);
        MultiMap<Abstraction, Pair<Abstraction, Abstraction>> reuseSet = reuseSummaries.get(n);
        Pair<Abstraction, Abstraction> p = new Pair<>(d2, d3);

        return reuseSet.contains(d1, p);
    }

    protected Map<Unit, MultiMap<Abstraction, Pair<Abstraction, Abstraction>>> myIncoming(Abstraction d1, SootMethod m) {
        return myIncoming.get(new Pair<>(m, d1));
    }

    public AbstractingCollectionInfoflowSolver(AbstractInfoflowProblem problem, InterruptableExecutor executor) {
        super(problem, executor);
    }

    protected void processCall(PathEdge<Unit, Abstraction> edge) {
        final Abstraction d1 = edge.factAtSource();
        final Unit n = edge.getTarget(); // a call node; line 14...
        final Abstraction d2Prev = edge.factAtTarget();
        // Widen if needed
        final Abstraction d2 = widening == null ? d2Prev : widening.widen(d2Prev, n);
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

                        if (res != null && !res.isEmpty()) {
                            Collection<Unit> startPointsOf = icfg.getStartPointsOf(sCalledProcN);
                            // for each result node of the call-flow function
                            for (Abstraction d3 : res) {
                                if (memoryManager != null)
                                    d3 = memoryManager.handleGeneratedMemoryObject(d2, d3);
                                if (d3 == null)
                                    continue;

                                if (subsuming.hasContext(d3) && !d3.getAccessPath().isStaticFieldRef()) {
                                    if (isReusable(sCalledProcN, d3)) {
                                        Abstraction prevSeenAbs = incomingWContext.putIfAbsent(new NoContextKey(sCalledProcN, d3), d3);
                                        if (prevSeenAbs == null) {
                                            if (d1 != zeroValue)
                                                prevContext.put(d3, new Pair<>(icfg.getMethodOf(n), d1));

                                            if (!addIncoming(sCalledProcN, d3, n, d1, d2, d3))
                                                continue;

                                            for (Unit sP : startPointsOf) {
                                                // create initial self-loop
                                                schedulingStrategy.propagateCallFlow(d3, sP, d3, n, false); // line 15
                                            }

                                            applyEndSummaryOnCallWith(d1, n, d2, returnSiteNs, sCalledProcN, d3, d3);
                                        } else {
                                            // Add the current abstraction into the incoming set of a similar abstraction
                                            addIncoming(sCalledProcN, prevSeenAbs, n, d1, d2, d3);

                                            // And do not propagate the abstraction in the caller but rather hope that
                                            // the similar abstraction can be reused in its summary
                                            applyEndSummaryOnCallWith(d1, n, d2, returnSiteNs, sCalledProcN, d3, prevSeenAbs);
                                        }
                                    } else {
                                        // We might encountered a callee, we already know is not reusable, on another
                                        // path. That means this path hasn't seen the context-dependent operation yet.
                                        // See testReinjectOnAlreadySeenCallee1.
                                        reinject(d1, icfg.getMethodOf(n));

                                        // register the fact that <sp,d3> has an incoming edge from
                                        // <n,d2>
                                        // line 15.1 of Naeem/Lhotak/Rodriguez
                                        if (!addIncoming(sCalledProcN, d3, n, d1, d2, d3))
                                            continue;

                                        for (Unit sP : startPointsOf) {
                                            // create initial self-loop
                                            schedulingStrategy.propagateCallFlow(d3, sP, d3, n, false); // line 15
                                        }

                                        applyEndSummaryOnCallWith(d1, n, d2, returnSiteNs, sCalledProcN, d3, d3);
                                    }
                                } else {
                                    // register the fact that <sp,d3> has an incoming edge from
                                    // <n,d2>
                                    // line 15.1 of Naeem/Lhotak/Rodriguez
                                    if (!addIncoming(sCalledProcN, d3, n, d1, d2, d3))
                                        continue;

                                    for (Unit sP : startPointsOf) {
                                        // create initial self-loop
                                        schedulingStrategy.propagateCallFlow(d3, sP, d3, n, false); // line 15
                                    }

                                    applyEndSummaryOnCallWith(d1, n, d2, returnSiteNs, sCalledProcN, d3, d3);
                                }
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
                        // Add result to check for widening
                        if (widening != null)
                            widening.recordNewFact(d3, n);

                        schedulingStrategy.propagateCallToReturnFlow(d1, returnSiteN, d3, n, true);
                    }
                }
            }
        }
    }

    @Override
    public void injectContext(IInfoflowSolver otherSolver, SootMethod callee, Abstraction d3, Unit callSite,
                              Abstraction d2, Abstraction d1) {
        if (!addIncoming(callee, d3, callSite, d1, d2, d3))
            return;

        Collection<Unit> returnSiteNs = icfg.getReturnSitesOfCallAt(callSite);
        applyEndSummaryOnCallWith(d1, callSite, d2, returnSiteNs, callee, d3, d3);
    }

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

        Map<Unit, MultiMap<Abstraction, Pair<Abstraction, Abstraction>>> inc = myIncoming(d1, methodThatNeedsSummary);

        // for each incoming call edge already processed
        // (see processCall(..))
        if (inc != null && !inc.isEmpty())
            for (Map.Entry<Unit, MultiMap<Abstraction, Pair<Abstraction, Abstraction>>> entry : inc.entrySet()) {
                // Early termination check
                if (killFlag != null)
                    return;

                // line 22
                Unit c = entry.getKey();
                Set<Abstraction> callerSideDs = entry.getValue().keySet();
                // for each return site
                for (Unit retSiteC : icfg.getReturnSitesOfCallAt(c)) {
                    // compute return-flow function
                    FlowFunction<Abstraction> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary, n,
                            retSiteC);
                        for (final Abstraction d4 : entry.getValue().keySet()) {
                            for (final Pair<Abstraction, Abstraction> pair : entry.getValue().get(d4)) {
                                Abstraction predVal = pair.getO1(); // d2 in the call flow
                                Abstraction narrowAbs = pair.getO2(); // d3 in the call flow i.e. d1 here

                                Abstraction d2new = d2;
                                Abstraction d1new = d1;
                                Abstraction d4new = d4;
                                if (d1 != narrowAbs) {
                                    d1new = narrowAbs; // Replace the calling context
                                    // Special case: if we have an identity flow, we can skip diffing access paths
                                    d2new = d1.equals(d2) ? narrowAbs : subsuming.applyDiffOf(d1, d2, narrowAbs);
                                    d4new = predVal;
                                }

                                // for each incoming-call value
                                Set<Abstraction> targets = computeReturnFlowFunction(retFunction, d1new, d2new, c, callerSideDs);
                                if (targets != null && !targets.isEmpty()) {
                                for (Abstraction d5 : targets) {
                                    if (memoryManager != null)
                                        d5 = memoryManager.handleGeneratedMemoryObject(d2new, d5);
                                    if (d5 == null)
                                        continue;

                                    // If we have not changed anything in the callee, we do not need the facts from
                                    // there. Even if we change something: If we don't need the concrete path, we
                                    // can skip the callee in the predecessor chain
                                    Abstraction d5p = shortenPredecessors(d5, predVal, d1new, n, c);
                                    schedulingStrategy.propagateReturnFlow(d4new, retSiteC, d5p, c, false);
                                }
                            }
                        }
                    }
                }
            }

        // handling for unbalanced problems where we return out of a method with
        // a fact for which we have no incoming flow
        // note: we propagate that way only values that originate from ZERO, as
        // conditionally generated values should only be propagated into callers that
        // have an incoming edge for this condition
        if (followReturnsPastSeeds && d1 == zeroValue && (inc == null || inc.isEmpty())) {
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
            final Map<Unit, MultiMap<Abstraction, Pair<Abstraction, Abstraction>>> inc = myIncoming(d1, methodThatNeedsSummary);

            if (inc == null || inc.isEmpty())
                followReturnsPastSeedsHandler.handleFollowReturnsPastSeeds(d1, u, d2);
        }
    }

    @Override
    protected void applyEndSummaryOnCall(final Abstraction d1, final Unit n, final Abstraction d2, Collection<Unit> returnSiteNs,
                                             SootMethod sCalledProcN, Abstraction d3) {
        throw new RuntimeException("Use applyEndSummaryOnCallWith instead");
    }

    private void reinject(Abstraction sourceVal, SootMethod sm) {
        // We need two passes here to prevent a race on notReusable and appending using incomingWContext

        Deque<Pair<SootMethod, Abstraction>> firstVisit = new ArrayDeque<>();
        firstVisit.add(new Pair<>(sm, sourceVal));
        // First pass: mark as methods in the call tree as not reusable
        while (!firstVisit.isEmpty()) {
            Pair<SootMethod, Abstraction> p = firstVisit.poll();
            Abstraction curr = p.getO2();
            SootMethod currMethod = p.getO1();

            markNotReusable(currMethod, curr);

            firstVisit.addAll(prevContext.get(curr));
        }

        Deque<Pair<SootMethod, Abstraction>> secondVisit = new ArrayDeque<>();
        secondVisit.add(new Pair<>(sm, sourceVal));
        // Second pass: reinject all taints that were appended to non-reusable taints
        while (!secondVisit.isEmpty()) {
            Pair<SootMethod, Abstraction> p = secondVisit.poll();
            Abstraction curr = p.getO2();
            SootMethod currMethod = p.getO1();

            secondVisit.addAll(prevContext.get(curr));

            Map<Unit, MultiMap<Abstraction, Pair<Abstraction, Abstraction>>> inc = myIncoming(curr, currMethod);
            if (inc != null && !inc.isEmpty()) {
                for (Unit u : inc.keySet()) {
                    MultiMap<Abstraction, Pair<Abstraction, Abstraction>> map = inc.get(u);
                    if (map == null)
                        continue;

                    for (Abstraction d1 : map.keySet()) {
                        Set<Pair<Abstraction, Abstraction>> pairs = map.get(d1);
                        if (pairs == null)
                            break;
                        for (Pair<Abstraction, Abstraction> pair : pairs) {
                            Abstraction d2 = pair.getO1();
                            Abstraction d3 = pair.getO2();
                            // The exemplary context propagated in the callee is still valid, only
                            // all similar abstractions are not
                            if (d3 != curr) {
                                // Reinject the similar abstractions at the method start
                                if (!removeAndAddIncoming(currMethod, curr, u, d1, d2, d3))
                                    continue;

                                for (Unit sP : icfg.getStartPointsOf(currMethod)) {
                                    // create initial self-loop
                                    schedulingStrategy.propagateCallFlow(d3, sP, d3, u, false); // line 15
                                }

                                applyEndSummaryOnCallWith(d1, u, d2, icfg.getReturnSitesOfCallAt(u), currMethod, d3, d3);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void applyEndSummaryOnCallWith(final Abstraction d1, final Unit n, final Abstraction d2, Collection<Unit> returnSiteNs,
                                     SootMethod sCalledProcN, Abstraction d3, Abstraction summaryQuery) {
        // line 15.2
        Set<EndSummary<Unit, Abstraction>> endSumm = endSummary(sCalledProcN, summaryQuery);
        boolean reuseOfSummary = d3 != summaryQuery;

        // If the combination of (method, parameter) is not reusable, exit here
        if (!isReusable(sCalledProcN, d3)) {
            return;
        }

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
                // to represent our initial summary again
                if (reuseOfSummary) {
                    // Special case: if we have an identity flow, we can skip diffing access paths
                    d4 = d4.equals(summaryQuery) ? d3 : subsuming.applyDiffOf(summaryQuery, d4, d3);
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
        }
    }

    @Override
    protected void propagate(Abstraction sourceVal, Unit target, Abstraction targetVal,
            /* deliberately exposed to clients */ Unit relatedCallSite,
            /* deliberately exposed to clients */ boolean isUnbalancedReturn, ScheduleTarget scheduleTarget) {
        // Mark (method, param) as not reusable if needed
        if (sourceVal != zeroValue && subsuming.affectsContext(target)) {
            SootMethod sm = icfg.getMethodOf(target);
            // We do not want to reinject everytime we see a context-dependent operation,
            // but only when we encounter a new not-reusable method summary.
            if (isReusable(sm, sourceVal)) {
                reinject(sourceVal, sm);
            }
        }

        super.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn, scheduleTarget);
    }

    protected void markNotReusable(SootMethod sm, Abstraction context) {
        notReusable.add(new Pair<>(sm, context.getAccessPath().getPlainValue()));
    }

    protected boolean isReusable(SootMethod sm, Abstraction context) {
        return !notReusable.contains(new Pair<>(sm, context.getAccessPath().getPlainValue()));
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.myIncoming.clear();
        this.notReusable.clear();
        this.incomingWContext.clear();
        this.prevContext.clear();
    }
}
