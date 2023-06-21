package soot.jimple.infoflow.collections.operations.forward.alias;

import java.util.Collection;

import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.operations.forward.InsertOperation;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;
import soot.jimple.infoflow.typing.TypeUtils;

public class AliasInsertOperation extends InsertOperation {

    public AliasInsertOperation(Location[] locations, int data, String field) {
        super(locations, data, field);
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager,
                         IContainerStrategy strategy, Collection<Abstraction> out) {
        InstanceInvokeExpr iie = (InstanceInvokeExpr) stmt.getInvokeExpr();

        if (manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getBase())) {
            AccessPathFragment fragment = getFragment(incoming);
            if (fragment == null)
                return false;

            Tristate state = matchContexts(fragment, iie, stmt, strategy, null, true);

            // Context doesn't match up
            if (state.isFalse())
                return false;

            // Taint the parameter
            {
                Value value = getValueFromIndex(data, stmt);
                if (!(value instanceof Constant)) {
                    AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(incoming.getAccessPath(), value, value.getType(), true);
                    Abstraction abs = incoming.deriveNewAbstraction(ap, stmt);
                    if (abs != null)
                        out.add(abs);
                }
            }

            // Return value
            if (stmt instanceof AssignStmt) {
                Value leftOp = ((AssignStmt) stmt).getLeftOp();

                if (!TypeUtils.isPrimitiveOrString(leftOp, incoming)) {
                    AccessPath ap = taintReturnValue(((AssignStmt) stmt).getLeftOp(), null, null,
                            incoming, strategy, manager);
                    if (ap == null)
                        return false;
                    Abstraction abs = incoming.deriveNewAbstraction(ap, stmt);
                    out.add(abs);
                }
            }

            // The collection is also tainted upwards
            return false;
        }

        // Left Op to collection in aliasing
        if (stmt instanceof AssignStmt) {
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
        }

        return super.apply(d1, incoming, stmt, manager, strategy, out);
    }
}
