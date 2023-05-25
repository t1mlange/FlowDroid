package soot.jimple.infoflow.collections.operations.forward;

import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.operations.LocationDependentOperation;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.ArrayList;
import java.util.Collection;

public class AccessOperation extends LocationDependentOperation {
    private final String returnField;

    public AccessOperation(Location[] keys, String field, String returnField) {
        super(keys, field);
        this.returnField = returnField;
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt,
                         InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        // No need to model an access if the return value is ignored
        if (!(stmt instanceof AssignStmt))
            return false;

        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getBase()))
            return false;

        AccessPathFragment fragment = getFragment(incoming);
        if (fragment == null)
            return false;

        ArrayList<ContextDefinition> copied = new ArrayList<>();
        Tristate state = matchContexts(fragment, iie, stmt, strategy, copied);

        if (!state.isFalse()) {
            Value leftOp = ((AssignStmt) stmt).getLeftOp();
            ContextDefinition[] newCtxt = copied.toArray(new ContextDefinition[0]);
            AccessPath ap = taintReturnValue(leftOp, newCtxt, returnField, incoming, strategy, manager);
            if (ap != null)
                out.add(incoming.deriveNewAbstraction(ap, stmt));
        }

        // Access never removes an element
        return false;
    }
}
