package soot.jimple.infoflow.collections.strategies.appending;

import soot.jimple.infoflow.collections.solver.fastSolver.AppendingCollectionInfoflowSolver;
import soot.jimple.infoflow.data.Abstraction;

/**
 * Strategy providing the {@link AppendingCollectionInfoflowSolver}
 * with the information needed to append similar collection abstractions to each other and only reinject them if the
 * callee is discovered to be concrete key-dependent.
 *
 * @param <N> statements
 * @param <D> fact domain
 */
public interface AppendingStrategy<N, D> {
    boolean hasContext(D fact);

    boolean affectsContext(N stmt);

    Abstraction applyDiffOf(D d1, D d2, D targetVal);
}
