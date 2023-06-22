package soot.jimple.infoflow.collections.solver.fastSolver;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import heros.FlowFunction;
import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.util.MultiMap;

/**
 * Infoflow Solver that supports various optimizations for precisely tracking collection keys/indices
 * - Supports widening on CallToReturn edges
 * - Tracks whether a ContextDefinition might be affected and
 *
 * @author Tim Lange
 */
public class CollectionInfoflowSolverPartSum extends CollectionInfoflowSolver {
    public CollectionInfoflowSolverPartSum(AbstractInfoflowProblem problem, InterruptableExecutor executor) {
        super(problem, executor);
    }

    // Calling Context -> (N, D)
    private MultiMap<NoContextKey, Pair<Unit, Abstraction>> callbacks;

    LoadingCache<SootMethod, Boolean> dependsOnContext = DEFAULT_CACHE_BUILDER.build(new CacheLoader<>() {
        @Override
        public Boolean load(SootMethod m) throws Exception {
            return ContainerDependenceAnalysis.analyze(icfg.getOrCreateUnitGraph(m), subsuming);
        }
    });

    protected void processCall(PathEdge<Unit, Abstraction> edge) {
        final Abstraction d1 = edge.factAtSource();
        final Unit n = edge.getTarget(); // a call node; line 14...
        final Abstraction d2Prev = edge.factAtTarget();
        // Widen if needed
        final Abstraction d2 = widening == null ? d2Prev : widening.widen(d2Prev, n);
        assert d2 != null;
        Collection<Unit> returnSiteNs = icfg.getReturnSitesOfCallAt(n);
        SootMethod caller = icfg.getMethodOf(n);

        // If our calling context depends on some context definition and
        // this statement might affect the context, then we want to save that
        // for later
        if (subsuming.hasContext(d1) && subsuming.affectsContext(n)) {
            callbacks.put(new NoContextKey(caller, d1), new Pair<>(n, d2));
        }

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

//                                if (!dependsOnContext.getUnchecked(sCalledProcN)) {
//                                    // If this method does not depend on the context, we can abstract
//                                    // away the contexts for the calling context and only add them later
//                                    // at the return statement
//                                    widening.forceWiden()
//                                }
//                                if (cb != null) {
                                    // If we have callbacks, we can just skip here
//                                    for (Pair<Unit, Abstraction> p : cb) {
//                                        Abstraction imm = p.getO2();
//
////                                        schedulingStrategy.propagateCallFlow(d3, p.getO1(), _, n, false);
//                                    }
//                                } else {
                                    // for each callee's start point(s)
                                    for (Unit sP : startPointsOf) {
                                        // create initial self-loop
                                        schedulingStrategy.propagateCallFlow(d3, sP, d3, n, false); // line 15
                                    }
//                                }

                                // register the fact that <sp,d3> has an incoming edge from
                                // <n,d2>
                                // line 15.1 of Naeem/Lhotak/Rodriguez
                                if (!addIncoming(sCalledProcN, d3, n, d1, d2))
                                    continue;

                                applyEndSummaryOnCall(d1, n, d2, returnSiteNs, sCalledProcN, d3);
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
}
