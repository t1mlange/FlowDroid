package soot.jimple.infoflow.collections.operations;

import heros.solver.PathEdge;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.CollectionTaintWrapper;
import soot.jimple.infoflow.collections.context.WildcardContext;
import soot.jimple.infoflow.collections.data.Index;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Collection;

public class ComputeOperation implements ICollectionOperation {
    private final int[] keys;
    private final String field;
    private final String fieldType;

    public ComputeOperation(int[] keys, String field, String fieldType) {
        this.keys = keys;
        this.field = field;
        this.fieldType = fieldType;
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getBase()))
            return false;

        AccessPathFragment fragment = incoming.getAccessPath().getFirstFragment();
        if (!fragment.getField().getSignature().equals(this.field))
            return false;

        Tristate state = Tristate.TRUE();
        // We might see a smashed container, where we can't infer anything
        if (fragment.hasContext()) {
            ContextDefinition[] apCtxt = fragment.getContext();
            assert keys.length == apCtxt.length; // Failure must be because of a bad model

            for (int i = 0; i < keys.length && state.isTrue(); i++) {
                ContextDefinition stmtKey = keys[i] == Index.ALL.toInt() ? WildcardContext.v() : strategy.getContextFromKey(iie.getArg(keys[i]), stmt);
                state = state.and(strategy.intersect(apCtxt[i], stmtKey));
            }
        }

        // If we are sure the collection is not tainted here, we can skip the callback handling
        if (state.isFalse())
            return false;

        SootMethod sm = strategy.getSootMethodFromValue(iie.getArg(1), stmt);
        if (sm != null) {
            analyzeCallback(d1, incoming, stmt, sm, manager);
        }

        // We first kill it and wait for the result
        return true;
    }

    public void analyzeCallback(Abstraction d1, Abstraction curr, Stmt stmt, SootMethod sm, InfoflowManager manager) {
        if (!sm.hasActiveBody()) {
            synchronized (sm) {
                if (!sm.hasActiveBody()) {
                    sm.retrieveActiveBody();
                }
            }
        }
        manager.getICFG().notifyMethodChanged(sm);

        Local p1 = sm.getActiveBody().getParameterLocal(1);
        AccessPath ap = manager.getAccessPathFactory().createAccessPath(p1, true);
        Abstraction abs = new Abstraction(null, ap, null, null, false, false);

        CollectionTaintWrapper tw = (CollectionTaintWrapper) manager.getTaintWrapper();
        tw.registerCallback(d1, stmt, curr, abs);

        for (Unit sP : manager.getICFG().getStartPointsOf(sm)) {
            PathEdge<Unit, Abstraction> edge = new PathEdge<>(abs, sP, abs);
            manager.getMainSolver().processEdge(edge);
        }
    }
}
