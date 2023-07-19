package soot.jimple.infoflow.collections.operations.forward;

import java.util.ArrayList;
import java.util.Collection;

import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.operations.LocationDependentOperation;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

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
        Value leftOp = ((AssignStmt) stmt).getLeftOp();

        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getBase()))
            return false;

        AccessPathFragment fragment = getFragment(incoming);
        if (fragment == null)
            return false;

        // For example, an alias.get("key") is only valid after the orig.put("key", tainted). Thus,
        // in general, accesses are prohibited for inactive abstractions (see testNoAccessAsInactive1).
        // Though, there might a map.put...map.get flow with a heap object that has a tainted field.
        // In this case, we need to allow accesses to find the alias. This is the corresponding check
        // to line 227-229 in InfoflowProblem. See also testAccessAlias1 in AliasMapTestCode, which needs
        // access in the inactive flow and testNoAccessAsInactive1.
        boolean invalidAccess = !incoming.isAbstractionActive()
                && incoming.getAccessPath().getFragmentCount() == 1
                && !incoming.dependsOnCutAP();
        if (invalidAccess)
            return false;

        ArrayList<ContextDefinition> copied = new ArrayList<>();
        Tristate state = matchContexts(fragment, iie, stmt, strategy, copied);

        if (!state.isFalse()) {
            ContextDefinition[] newCtxt = copied.toArray(new ContextDefinition[0]);
            AccessPath ap = taintReturnValue(leftOp, newCtxt, returnField, incoming, strategy, manager);
            if (ap != null)
                out.add(incoming.deriveNewAbstraction(ap, stmt));
        }

        // Access never removes an element
        return false;
    }
}
