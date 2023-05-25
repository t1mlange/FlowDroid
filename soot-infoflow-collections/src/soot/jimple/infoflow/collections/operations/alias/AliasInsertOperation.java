package soot.jimple.infoflow.collections.operations.alias;

import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.operations.LocationDependentOperation;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Collection;

public class AliasInsertOperation extends LocationDependentOperation {

    public AliasInsertOperation(Location[] locations, String field) {
        super(locations, field);
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        
        return false;
    }
}
