package soot.jimple.infoflow.collections.strategies.subsuming;

import java.util.Set;

import soot.jimple.infoflow.data.Abstraction;

public interface SubsumingStrategy<N, D> {

    /**
     * Defines an ordering of data flow facts to provide a priority for the worklist.
     * Must be thread-safe and should be fast
     *
     * @param a data flow fact
     * @param b data flow fact
     * @return compare result
     */
    int compare(D a, D b);

    /**
     * Checks whether A subsumes B
     *
     * @param a data flow fact
     * @param b data flow fact
     * @return true if a subsumes b
     */
    boolean subsumes(D a, D b);

    boolean hasContext(D fact);

    D removeContext(D fact);

    D chooseContext(Set<D> availableContexts, D currentContext);

    boolean affectsContext(D incoming, D outgoing);
    boolean affectsContext(N stmt);

    Abstraction applyDiffOf(Abstraction d1, Abstraction d2, Abstraction targetVal);
}
