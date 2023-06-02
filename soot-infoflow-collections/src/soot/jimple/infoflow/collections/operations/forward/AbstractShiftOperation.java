package soot.jimple.infoflow.collections.operations.forward;

import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.data.ParamIndex;
import soot.jimple.infoflow.collections.operations.LocationDependentOperation;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Collection;

public abstract class AbstractShiftOperation extends LocationDependentOperation {

    public AbstractShiftOperation(Location[] keys, String field) {
        super(keys, field);
        assert keys.length == 1; // TODO: generalize
    }

    protected abstract ContextDefinition shift(ContextDefinition ctxt, Stmt stmt, boolean exact, IContainerStrategy strategy);

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

        int idx = locations[0].getParamIdx();
        Tristate t;
        if (idx == ParamIndex.ALL) {
            t = Tristate.MAYBE();
        } else if (idx >= 0) {
            ContextDefinition stmtCtxt = strategy.getIndexContext(iie.getArg(locations[0].getParamIdx()), stmt);
            t = strategy.lessThanEqual(stmtCtxt, ctxt);
        } else {
            throw new RuntimeException("Unexpected shift index: " + idx);
        }

        // If the insert might be in front of this index, we have to shift
        if (!t.isFalse()) {
            ContextDefinition newCtxt = shift(ctxt, stmt, t.isTrue(), strategy);
            AccessPathFragment[] oldFragments = incoming.getAccessPath().getFragments();
            AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length];
            System.arraycopy(oldFragments, 0, fragments, 1, fragments.length - 1);
            if (newCtxt == UnknownContext.v())
                fragments[0] = fragment.copyWithNewContext(null);
            else
                fragments[0] = fragment.copyWithNewContext(new ContextDefinition[]{ newCtxt });
            AccessPath ap = manager.getAccessPathFactory().createAccessPath(base, fragments, incoming.getAccessPath().getTaintSubFields());
            if (ap != null)
                out.add(incoming.deriveNewAbstraction(ap, stmt));
            return true;
        }

        return false;
    }
}
