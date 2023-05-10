package soot.jimple.infoflow.collections.operations;

import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Collection;

/**
 * Common interface for all modelled operations on collections
 *
 * @author Tim Lange
 */
public interface ICollectionOperation {
    /**
     * Applies the operation to a taint.
     *
     * @param stmt current statement
     * @param incoming incoming taint
     * @param out outgoing set to which add new taints
     * @param manager infoflow manager
     * @param keyStrategy strategy that provides the possible location abstractions
     * @return true if the incoming abstraction should be killed
     */
    boolean apply(Stmt stmt, Abstraction incoming, Collection<Abstraction> out,
                  InfoflowManager manager, IContainerStrategy keyStrategy);
}
