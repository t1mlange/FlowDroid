package soot.jimple.infoflow.collections.operations;

import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Collection;

public class ShiftOperation extends LocationDependentOperation {

    public ShiftOperation(Location[] keys, String field, String fieldType) {
        super(keys, field, fieldType);
        assert keys.length == 1; // TODO: generalize
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
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
        ContextDefinition stmtCtxt = strategy.getIndexContext(iie.getArg(keys[0].getParamIdx()), stmt);
        Tristate t = strategy.lessThanEqual(stmtCtxt, ctxt);

        // If the insert might be in front of this index, we have to shift
        if (!t.isFalse()) {
            ContextDefinition newCtxt = strategy.shiftRight(ctxt, stmt, t.isTrue());
            AccessPathFragment[] oldFragments = incoming.getAccessPath().getFragments();
            AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length];
            System.arraycopy(oldFragments, 0, fragments, 1, fragments.length - 1);
            if (newCtxt == UnknownContext.v())
                fragments[0] = fragment.copyWithNewContext(null);
            else
                fragments[0] = fragment.copyWithNewContext(new ContextDefinition[]{ newCtxt });
            AccessPath ap = manager.getAccessPathFactory().createAccessPath(base, fragments, incoming.getAccessPath().getTaintSubFields());
            out.add(incoming.deriveNewAbstraction(ap, stmt));
            return true;
        }

        return false;
    }
}
