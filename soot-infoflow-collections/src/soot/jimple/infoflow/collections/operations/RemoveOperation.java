package soot.jimple.infoflow.collections.operations;

import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.strategies.IKeyStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Collection;

public class RemoveOperation implements ICollectionOperation {
    private final int[] keys;
    private final boolean allKeys;
    private final boolean returnOldElement;

    public RemoveOperation(int[] keys, boolean allKeys, boolean returnOldElement) {
        this.allKeys = allKeys;
        this.keys = allKeys ? null : keys;
        this.returnOldElement = returnOldElement;
    }

    @Override
    public boolean apply(Stmt stmt, Abstraction incoming, Collection<Abstraction> out, InfoflowManager manager, IKeyStrategy strategy) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getBase()))
            return false;

        Tristate state = Tristate.TRUE();
        // If we remove unconditionally, we can skip checking the keys
        if (!allKeys) {
            ContextDefinition apKey = incoming.getAccessPath().getFirstFragment().getContext();
            for (int keyIdx : keys) {
                ContextDefinition stmtKey = strategy.getFromValue(iie.getArg(keyIdx));
                state = state.and(strategy.intersect(apKey, stmtKey));
            }
        }

        return state.isTrue();
    }
}
