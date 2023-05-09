package soot.jimple.infoflow.collections.operations;

import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.strategies.IKeyStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Arrays;
import java.util.Collection;

public class AccessOperation extends AbstractOperation {
    private final int[] keys;

    public AccessOperation(int[] keys) {
        this.keys = keys;
    }

    @Override
    public boolean apply(Stmt stmt,
                         Abstraction incoming,
                         Collection<Abstraction> out,
                         InfoflowManager manager,
                         IKeyStrategy strategy) {
        // No need to model an access if the return value is ignored
        if (!(stmt instanceof AssignStmt))
            return false;

        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getBase()))
            return false;

        Tristate state = Tristate.TRUE();
        for (int keyIdx : keys) {
            ContextDefinition stmtKey = strategy.getFromValue(iie.getArg(keyIdx));
            ContextDefinition apKey = incoming.getAccessPath().getFirstFragment().getContext();
            state = state.and(strategy.intersect(apKey, stmtKey));

            // If we definitely do not access an element, we can stop here
            if (state.isFalse())
                break;
        }

        if (!state.isFalse()) {
            Value leftOp = ((AssignStmt) stmt).getLeftOp();
            AccessPathFragment[] oldFragments = incoming.getAccessPath().getFragments();
            AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length - 1];
            System.arraycopy(oldFragments, 1, fragments, 0, fragments.length);
            AccessPath ap = manager.getAccessPathFactory().createAccessPath(leftOp, fragments, incoming.getAccessPath().getTaintSubFields());
            out.add(incoming.deriveNewAbstraction(ap, stmt));
        }

        // Access never removes an element
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessOperation that = (AccessOperation) o;
        return Arrays.equals(keys, that.keys);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(keys);
    }

}
