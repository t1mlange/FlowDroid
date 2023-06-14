package soot.jimple.infoflow.collections.operations.forward.alias;

import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.operations.LocationDependentOperation;
 import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Collection;

public class AliasRemoveOperation extends LocationDependentOperation {
    public AliasRemoveOperation(Location[] keys, String field) {
        super(keys, field);
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        // Any remove above the set of the alias won't remove the element but the one
        // that was at that location before. See AliasMapTests#MapClear1.
        return false;
    }
}
