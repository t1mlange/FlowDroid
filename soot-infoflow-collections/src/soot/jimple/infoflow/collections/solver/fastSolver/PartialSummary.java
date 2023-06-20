package soot.jimple.infoflow.collections.solver.fastSolver;

import soot.jimple.infoflow.solver.EndSummary;
import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;

/**
 * Represents a summary that only summarizes a partial
 * @param <N>
 * @param <D>
 */
public class PartialSummary<N, D extends FastSolverLinkedNode<D, N>> extends EndSummary<N, D> {
    public PartialSummary(N eP, D d4, D calleeD1) {
        super(eP, d4, calleeD1);
    }
}
