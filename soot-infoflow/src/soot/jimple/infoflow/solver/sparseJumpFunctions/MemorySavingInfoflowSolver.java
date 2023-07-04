package soot.jimple.infoflow.solver.sparseJumpFunctions;

import heros.FlowFunction;
import heros.solver.PathEdge;
import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.fastSolver.InfoflowSolver;

import java.util.Set;

/**
 * The MemorySavingInfoflow solver does only sparsely populate the jump functions.'
 *
 * This means it will only add an edge to the graph if it is actually needed. AFAIK, these are only merge points,
 * i.e. start points of methods, return sites and normal flows with more than one predecessor.
 * Additionally, all initial seeds are needed as well.
 *
 * @author Tim Lange
 */
public class MemorySavingInfoflowSolver extends InfoflowSolver {
    public MemorySavingInfoflowSolver(AbstractInfoflowProblem problem, InterruptableExecutor executor) {
        super(problem, executor);
        this.setSchedulingStrategy(new MemorySavingSchedulingStrategy(this).EACH_EDGE_INDIVIDUALLY);
    }

    /**
     * Lines 33-37 of the algorithm. Simply propagate normal, intra-procedural
     * flows.
     *
     * @param edge
     */
    protected void processNormalFlow(PathEdge<Unit, Abstraction> edge) {
        final Abstraction d1 = edge.factAtSource();
        final Unit n = edge.getTarget();
        final Abstraction d2 = edge.factAtTarget();

        for (Unit m : icfg.getSuccsOf(n)) {
            // Early termination check
            if (killFlag != null)
                return;

            // Compute the flow function
            FlowFunction<Abstraction> flowFunction = flowFunctions.getNormalFlowFunction(n, m);
            Set<Abstraction> res = computeNormalFlowFunction(flowFunction, d1, d2);
            if (res != null && !res.isEmpty()) {
                for (Abstraction d3 : res) {
                    if (memoryManager != null && d2 != d3)
                        d3 = memoryManager.handleGeneratedMemoryObject(d2, d3);
                    if (d3 != null)
                        ((MemorySavingSchedulingStrategy.MYSCHED) schedulingStrategy)
                                .propagateNormalFlow(d1, m, d3, null, false, d2 == d3);
                }
            }
        }
    }

    public void propagate(Abstraction sourceVal, Unit target, Abstraction targetVal,
            /* deliberately exposed to clients */ Unit relatedCallSite,
            /* deliberately exposed to clients */ boolean isUnbalancedReturn, ScheduleTarget scheduleTarget,
                             boolean doSave) {
        // Let the memory manager run
        if (memoryManager != null) {
            sourceVal = memoryManager.handleMemoryObject(sourceVal);
            targetVal = memoryManager.handleMemoryObject(targetVal);
            if (targetVal == null)
                return;
        }

        // Check the path length
        if (maxAbstractionPathLength >= 0 && targetVal.getPathLength() > maxAbstractionPathLength)
            return;

        if (doSave) {
            final PathEdge<Unit, Abstraction> edge = new PathEdge<>(sourceVal, target, targetVal);
            final Abstraction existingVal = addFunction(edge);
            if (existingVal != null) {
                if (existingVal != targetVal) {
                    // Check whether we need to retain this abstraction
                    boolean isEssential;
                    if (memoryManager == null)
                        isEssential = relatedCallSite != null && icfg.isCallStmt(relatedCallSite);
                    else
                        isEssential = memoryManager.isEssentialJoinPoint(targetVal, relatedCallSite);

                    if (maxJoinPointAbstractions < 0 || existingVal.getNeighborCount() < maxJoinPointAbstractions
                            || isEssential) {
                        existingVal.addNeighbor(targetVal);
                    }
                }
            } else {
                scheduleEdgeProcessing(edge, scheduleTarget);
            }
        } else {
            final PathEdge<Unit, Abstraction> edge = new PathEdge<>(sourceVal, target, targetVal);
            scheduleEdgeProcessing(edge, scheduleTarget);
        }
    }
}
