package soot.jimple.infoflow.collections.operations.forward.alias;

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
import soot.jimple.infoflow.typing.TypeUtils;

public class AliasComputeOperation extends LocationDependentOperation {

    public AliasComputeOperation(Location[] locations, String field, int data,
                                 int callbackIdx, int callbackBaseIdx, int callbackDataIdx,
                                 boolean doReturn) {
        super(locations, field);
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager,
                         IContainerStrategy strategy, Collection<Abstraction> out) {
        if (stmt instanceof AssignStmt) {
            Value leftOp = ((AssignStmt) stmt).getLeftOp();
            InstanceInvokeExpr iie = (InstanceInvokeExpr) stmt.getInvokeExpr();

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
            } else if (!TypeUtils.isPrimitiveOrString(leftOp, incoming)
                        && manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getBase())) {
                // If the collection is tainted and the lhs is on the heap, we have found an alias.
                AccessPathFragment fragment = getFragment(incoming);
                if (fragment == null)
                    return false;

                Tristate state = matchContexts(fragment, iie, stmt, strategy, null);

                // Context doesn't match up
                if (state.isFalse())
                    return false;

                AccessPath ap = taintReturnValue(((AssignStmt) stmt).getLeftOp(), null, null,
                        incoming, strategy, manager);
                if (ap == null)
                    return false;
                Abstraction abs = incoming.deriveNewAbstraction(ap, stmt);
                out.add(abs);

                // The collection is also tainted upwards
                return false;
            }
        }

        return false;
    }
}
