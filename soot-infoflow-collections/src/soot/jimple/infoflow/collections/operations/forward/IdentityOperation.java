package soot.jimple.infoflow.collections.operations.forward;

import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.operations.ICollectionOperation;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Collection;

/**
 * An operation that does not affect the flow, yet marks the method as handled.
 * Useful for alias operations in some cases:
 * 1. Shift: The index analysis already retrieved the correct index. We only want to find aliases to the list.
 * 2. Invalidate: Whether a list is sorted before the insert is irrelevant for the index and thus, needs no handling.
 * 3. Remove: remove's don't have any effect on the taint when we search for aliases. Note that remove here means
 *            remove operations, returned elements of the remove method are still done through the AccessOperation.
 *
 * @author Tim Lange
 */
public class IdentityOperation implements ICollectionOperation {
    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        return false;
    }
}
