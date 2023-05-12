package soot.jimple.infoflow.collections.operations;

import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Collection;

public class ShiftOperation implements ICollectionOperation {
    private final int key;
    private final String field;
    private final String fieldType;

    public ShiftOperation(int[] keys, String field, String fieldType) {
        assert keys.length == 1;
        this.key = keys[0];
        this.field = field;
        this.fieldType = fieldType;
    }

    @Override
    public boolean apply(Stmt stmt, Abstraction incoming, Collection<Abstraction> out, InfoflowManager manager, IContainerStrategy strategy) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        Value base = iie.getBase();
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), base))
            return false;

        AccessPathFragment fragment = incoming.getAccessPath().getFirstFragment();
        // Either when the field doesn't match or there is no index to shift, we can stop here
        if (!fragment.getField().getSignature().equals(this.field) || !fragment.hasContext())
            return false;

        ContextDefinition[] ctxts = fragment.getContext();
        assert ctxts.length == 1;

        ContextDefinition ctxt = ctxts[0];
        ContextDefinition stmtCtxt = strategy.getContextFromKey(iie.getArg(key), stmt);
        Tristate t = strategy.lessThan(ctxt, stmtCtxt);

        // If the insert might be in front of this index, we have to shift
        if (!t.isTrue()) {
            ContextDefinition newCtxt = strategy.shiftRight(ctxt);
            AccessPathFragment[] oldFragments = incoming.getAccessPath().getFragments();
            AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length];
            System.arraycopy(oldFragments, 0, fragments, 1, fragments.length - 1);
            fragments[0] = fragment.copyWithNewContext(new ContextDefinition[]{ newCtxt });
            AccessPath ap = manager.getAccessPathFactory().createAccessPath(base, fragments, incoming.getAccessPath().getTaintSubFields());
            out.add(incoming.deriveNewAbstraction(ap, stmt));
        }

        // We can kill the current index if we are sure the insert index was before
        return t.isFalse();
    }
}
