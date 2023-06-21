package soot.jimple.infoflow.collections.operations.forward;

import java.util.Collection;

import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.operations.LocationDependentOperation;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.ContextDefinition;

public class InsertOperation extends LocationDependentOperation {
    protected final int data;

    public InsertOperation(Location[] keys, int data, String field) {
        super(keys, field);
        this.data = data;
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getArg(data)))
            return false;

        ContextDefinition[] ctxt = buildContext(strategy, iie, stmt);
        AccessPath ap = taintCollectionWithContext(iie.getBase(), ctxt, incoming.getAccessPath(), manager);
        if (ap != null)
            out.add(incoming.deriveNewAbstraction(ap, stmt));

        // Insert never removes an element
        return false;
    }
}
