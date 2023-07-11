package soot.jimple.infoflow.collections.operations.forward.alias;

import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.operations.LocationDependentOperation;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Collection;


public class AliasShiftRightOperation extends LocationDependentOperation {
    public AliasShiftRightOperation(Location[] keys, String field) {
        super(keys, field);
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        // The list size analysis tells us the index in the taint analysis that is correct w.r.t to flows
        // and possible aliases. Shifts before the statement (i.e. the statements in the alias flow) do not
        // affect the index we queried at the point. We only search for other lists that may alias here.
        return false;
    }
}
