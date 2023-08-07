package soot.jimple.infoflow.collections.operations.forward;

import java.util.Collection;

import soot.Value;
import soot.jimple.AbstractImmediateSwitch;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.operations.LocationDependentOperation;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;
import soot.util.Switch;

public class InsertOperation extends LocationDependentOperation {
    protected final int data;
    protected final boolean append;

    public InsertOperation(Location[] keys, int data, String field, boolean append) {
        super(keys, field);
        this.data = data;
        this.append = append;
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getArg(data)))
            return false;

        ContextDefinition[] ctxt;
        if (append) {
            // We do not check here for :)
//            AccessPathFragment fragment = incoming.getAccessPath().getFirstFragment();
//            if (fragment == null || !fragment.hasContext()) {
//                // If our incoming list has no context, we can remove the context from
//                // the current list and
//                ctxt = null;
//            } else {
//                ctxt = strategy.union(buildContext(strategy, iie, stmt), fragment.getContext());
//            }
            ctxt = null;
        } else {
            ctxt = buildContext(strategy, iie, stmt);
        }
        AccessPath ap = taintCollectionWithContext(iie.getBase(), ctxt, incoming.getAccessPath(), manager);
        if (ap != null)
            out.add(incoming.deriveNewAbstraction(ap, stmt));

        // Insert never removes an element
        return false;
    }
}
