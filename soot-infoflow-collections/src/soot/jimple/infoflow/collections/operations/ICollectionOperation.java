package soot.jimple.infoflow.collections.operations;

import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Collection;

/**
 * Common interface for all modelled operations on collections
 *
 * @author Tim Lange
 */
public interface ICollectionOperation {
    /**
     * Applies the operation to a taint
     *
     * @param d1        current context
     * @param incoming  incoming taint
     * @param stmt      current statement
     * @param manager   infoflow manager
     * @param strategy  strategy that provides the possible location abstractions
     * @param out       outgoing set to which add new taints
     * @return true if the incoming abstraction should be killed
     */
    boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager,
                  IContainerStrategy strategy, Collection<Abstraction> out);
}
