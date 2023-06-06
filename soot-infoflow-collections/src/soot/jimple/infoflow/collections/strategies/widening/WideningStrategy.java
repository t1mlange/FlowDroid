package soot.jimple.infoflow.collections.strategies.widening;

import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;

/**
 * Provides the infoflow solver with information about when to widen.
 * Precondition: needs to be thread-safe
 *
 * @author Tim Lange
 */
public interface WideningStrategy<N, D extends FastSolverLinkedNode<D, N>> {
    /**
     * Callback called on each new fact created by a call to return flow function
     *
     * @param fact new fact
     * @param n program point
     */
    void recordNewFact(D fact, N n);

    /**
     * Widens the abstraction
     *
     * @param fact current fact
     * @param n program point
     * @return possibly widened fact
     */
    D widen(D fact, N n);
}
