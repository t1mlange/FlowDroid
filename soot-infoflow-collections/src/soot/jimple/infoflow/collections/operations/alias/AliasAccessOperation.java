package soot.jimple.infoflow.collections.operations.alias;

import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.operations.forward.AccessOperation;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.ContextDefinition;
import soot.jimple.infoflow.typing.TypeUtils;

import java.util.Collection;

public class AliasAccessOperation extends AccessOperation {
    public AliasAccessOperation(Location[] keys, String field, String returnField) {
        super(keys, field, returnField);
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        if (!(stmt instanceof AssignStmt))
            return false;

        if (!TypeUtils.isPrimitiveOrString(((AssignStmt) stmt).getLeftOp(), incoming))
            super.apply(d1, incoming, stmt, manager, strategy, out);

        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        Value leftOp = ((AssignStmt) stmt).getLeftOp();
        if (manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), leftOp)) {
            // If the lhs is tainted, we need to taint the collection
            ContextDefinition[] ctxt = buildContext(strategy, iie, stmt);
            AccessPath ap = taintCollectionWithContext(iie.getBase(), ctxt, incoming.getAccessPath(), manager);
            if (ap == null)
                return true;
            Abstraction abs = incoming.deriveNewAbstraction(ap, stmt);
            out.add(abs);

            // Returned value is written here
            return true;
        }

        return false;
    }
}
