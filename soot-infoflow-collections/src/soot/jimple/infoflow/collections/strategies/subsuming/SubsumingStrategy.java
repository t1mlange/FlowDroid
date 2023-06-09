package soot.jimple.infoflow.collections.strategies.subsuming;

public interface SubsumingStrategy<D> {

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
}
