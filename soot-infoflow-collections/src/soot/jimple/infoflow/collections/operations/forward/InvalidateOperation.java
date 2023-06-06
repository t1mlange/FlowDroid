package soot.jimple.infoflow.collections.operations.forward;

import java.util.Collection;

import soot.SootField;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.operations.AbstractOperation;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;

public class InvalidateOperation extends AbstractOperation {
    private final int data;
    private final String field;
    private final AccessPathTuple returnTuple;

    public InvalidateOperation(int data, String field, AccessPathTuple returnTuple) {
        this.data = data;
        this.field = field;
        this.returnTuple = returnTuple;
    }

    private Abstraction deriveReturnValueTaint(Stmt stmt, Abstraction incoming, InfoflowManager manager) {
        if (stmt instanceof AssignStmt) {
            Value leftOp = ((AssignStmt) stmt).getLeftOp();
            int oldSize = incoming.getAccessPath().getFragmentCount();
            String[] fields = returnTuple.getFields();
            AccessPathFragment[] newFragments = new AccessPathFragment[fields.length + oldSize - 1];
            System.arraycopy(incoming.getAccessPath().getFragments(), 1, newFragments, fields.length, oldSize - 1);
            for (int i = 0; i < fields.length; i++) {
                SootField f = safeGetField(fields[i]);
                newFragments[i] = new AccessPathFragment(f, f.getType());
            }
            AccessPath ap = manager.getAccessPathFactory().createAccessPath(leftOp, newFragments, incoming.getAccessPath().getTaintSubFields());
            return incoming.deriveNewAbstraction(ap, stmt);
        }

        return null;
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        Value val = getValueFromIndex(data, stmt);
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), val))
            return false;

        AccessPathFragment fragment = incoming.getAccessPath().getFirstFragment();
        if (fragment == null || !fragment.getField().getSignature().equals(this.field) || !fragment.hasContext())
            return false;

        AccessPathFragment[] oldFragments = incoming.getAccessPath().getFragments();
        AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length];
        System.arraycopy(oldFragments, 1, fragments, 1, fragments.length - 1);
        fragments[0] = oldFragments[0].copyWithNewContext(null);
        AccessPath ap = manager.getAccessPathFactory().createAccessPath(val, fragments, incoming.getAccessPath().getTaintSubFields());
        out.add(incoming.deriveNewAbstraction(ap, stmt));

        if (returnTuple != null) {
            Abstraction abs = deriveReturnValueTaint(stmt, incoming, manager);
            if (abs != null)
                out.add(abs);
        }

        // The newly created taint contains the old one, keeping the old one alive would be redundant
        return true;
    }
}
