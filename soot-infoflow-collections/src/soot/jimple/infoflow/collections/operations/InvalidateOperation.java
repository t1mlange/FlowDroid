package soot.jimple.infoflow.collections.operations;

import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Collection;

public class InvalidateOperation extends LocationDependentOperation {
    public InvalidateOperation(Location[] keys, String field, String fieldType) {
        super(keys, field, fieldType);
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        Value base = ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase();
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), base))
            return false;

        AccessPathFragment fragment = incoming.getAccessPath().getFirstFragment();
        if (!fragment.getField().getSignature().equals(this.field) || !fragment.hasContext())
            return false;

        ContextDefinition[] ctxt = fragment.getContext();
        for (Location key : keys) {
            // Invalidate the n-th key
            ctxt[key.getParamIdx()] = UnknownContext.v();
        }
        // Maybe fully smash the context if there's no useful information left
        if (strategy.shouldSmash(ctxt))
            ctxt = null;

        AccessPathFragment[] oldFragments = incoming.getAccessPath().getFragments();
        AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length];
        System.arraycopy(oldFragments, 1, fragments, 1, fragments.length);
        fragments[0] = oldFragments[0].copyWithNewContext(ctxt);
        AccessPath ap = manager.getAccessPathFactory().createAccessPath(base, fragments, incoming.getAccessPath().getTaintSubFields());
        out.add(incoming.deriveNewAbstraction(ap, stmt));

        // The newly created taint contains the old one, keeping the old one alive would be redundant
        return true;
    }
}
