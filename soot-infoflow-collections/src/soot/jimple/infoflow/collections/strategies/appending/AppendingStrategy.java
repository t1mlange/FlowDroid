package soot.jimple.infoflow.collections.strategies.appending;

import soot.SootField;
import soot.SootMethod;
import soot.jimple.infoflow.collections.solver.fastSolver.AppendingCollectionInfoflowSolver;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Collection;
import java.util.Collections;

/**
 * Strategy providing the {@link AppendingCollectionInfoflowSolver}
 * with the information needed to append similar collection abstractions to each other and only reinject them if the
 * callee is discovered to be concrete key-dependent.
 *
 * @param <N> statements
 * @param <D> fact domain
 */
public interface AppendingStrategy<N, D> {
    /**
     * Notify the appending strategy which fields might contain contexts and which methods affects the context.
     * Allows to skip bookkeeping on most fields.
     *
     * @param fields  Fields that might have a context
     * @param methods Methods that affect or use the context
     */
    void initialize(Collection<SootField> fields, Collection<SootMethod> methods);

    boolean hasContext(D fact);

    boolean affectsContext(N stmt);

    Abstraction applyDiffOf(D d1, D d2, D targetVal);
}
