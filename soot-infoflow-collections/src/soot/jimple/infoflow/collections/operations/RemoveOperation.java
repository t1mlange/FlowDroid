package soot.jimple.infoflow.collections.operations;

import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.data.ParamIndex;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Collection;

public class RemoveOperation extends LocationDependentOperation {
    public RemoveOperation(Location[] keys, String field, String fieldType) {
        super(keys, field, fieldType);
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getBase()))
            return false;

        AccessPathFragment fragment = incoming.getAccessPath().getFirstFragment();
        if (!fragment.getField().getSignature().equals(this.field))
            return false;

        // We might see a smashed container, where we can't infer anything
        if (!fragment.hasContext())
            return false;

        ContextDefinition[] apCtxt = fragment.getContext();
        assert keys.length == apCtxt.length; // Failure must be because of a bad model

        Tristate state = Tristate.TRUE();
        for (int i = 0; i < keys.length && state.isTrue(); i++) {
            // We do not have to check the context if the key is a wildcard anyway
            if (keys[i].getParamIdx() != ParamIndex.ALL.toInt()) {
                ContextDefinition stmtKey;
                if (keys[i].isValueBased())
                    stmtKey = strategy.getIndexContext(iie.getArg(keys[i].getParamIdx()), stmt);
                else
                    stmtKey = strategy.getKeyContext(iie.getArg(keys[i].getParamIdx()), stmt);
                state = state.and(strategy.intersect(apCtxt[i], stmtKey));
            }
        }

        return state.isTrue();
    }
}
