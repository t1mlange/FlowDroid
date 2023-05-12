package soot.jimple.infoflow.collections.operations;

import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.WildcardContext;
import soot.jimple.infoflow.collections.data.Index;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Collection;

public class RemoveOperation implements ICollectionOperation {
    private final int[] keys;
    private final String field;
    private final String fieldType;

    public RemoveOperation(int[] keys, String field, String fieldType) {
        this.keys = keys;
        this.field = field;
        this.fieldType = fieldType;
    }

    @Override
    public boolean apply(Stmt stmt, Abstraction incoming, Collection<Abstraction> out, InfoflowManager manager, IContainerStrategy strategy) {
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
            ContextDefinition stmtKey = keys[i] == Index.ALL.toInt() ? WildcardContext.v() : strategy.getContextFromKey(iie.getArg(keys[i]), stmt);
            state = state.and(strategy.intersect(apCtxt[i], stmtKey));
        }

        return state.isTrue();
    }
}
