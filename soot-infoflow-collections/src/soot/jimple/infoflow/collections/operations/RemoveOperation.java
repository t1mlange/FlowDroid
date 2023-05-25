package soot.jimple.infoflow.collections.operations;

import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPathFragment;

import java.util.Collection;

public class RemoveOperation extends LocationDependentOperation {
    public RemoveOperation(Location[] keys, String field) {
        super(keys, field);
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getBase()))
            return false;

        AccessPathFragment fragment = incoming.getAccessPath().getFirstFragment();
        if (!fragment.getField().getSignature().equals(this.field))
            return false;

        // For removal, we need to be pessimistic about matching the access path.
        // If we see a smashed collection, we can not kill the taint.
        if (!fragment.hasContext())
            return false;

        return matchContexts(fragment, iie, stmt, strategy, null).isTrue();
    }
}
