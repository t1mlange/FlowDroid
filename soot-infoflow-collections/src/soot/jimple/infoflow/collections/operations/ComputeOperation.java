package soot.jimple.infoflow.collections.operations;

import heros.solver.PathEdge;
import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.CollectionTaintWrapper;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.data.ParamIndex;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Collection;

public class ComputeOperation extends LocationDependentOperation {
    private final int callbackIdx;

    public ComputeOperation(Location[] keys, String field, String fieldType, int callbackIdx) {
        super(keys, field, fieldType);
        this.callbackIdx = callbackIdx;
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager,
                         IContainerStrategy strategy, Collection<Abstraction> out) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getBase()))
            return identityDependsOnCallback(d1, incoming, stmt, manager, strategy);
        if (manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getArg(callbackIdx)))
            isCallbackReturnAlwaysTainted(d1, incoming, stmt, manager, strategy);
        return false;
    }

    private boolean identityDependsOnCallback(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager,
                                              IContainerStrategy strategy) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());

        AccessPathFragment fragment = incoming.getAccessPath().getFirstFragment();
        if (!fragment.getField().getSignature().equals(this.field))
            return false;

        Tristate state = Tristate.TRUE();
        // We might see a smashed container, where we can't infer anything
        if (fragment.hasContext()) {
            ContextDefinition[] apCtxt = fragment.getContext();
            assert keys.length == apCtxt.length; // Failure must be because of a bad model

            for (int i = 0; i < keys.length && state.isTrue(); i++) {
                ContextDefinition stmtKey = strategy.getKeyContext(iie.getArg(keys[i].getParamIdx()), stmt);
                state = state.and(strategy.intersect(apCtxt[i], stmtKey));
            }
        }

        // If we are sure the collection is not tainted here, we can skip the callback handling
        if (state.isFalse())
            return false;

        // Analyze each callback present in the call-graph
        manager.getICFG().getCalleesOfCallAt(stmt).stream()
                .filter(m -> m.getName().equals("apply"))
                .forEach(m -> analyzeCallback(d1, incoming, stmt, m, manager));

        return true;
    }

    private void isCallbackReturnAlwaysTainted(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager,
                                               IContainerStrategy strategy) {
        manager.getICFG().getCalleesOfCallAt(stmt).stream()
                .filter(m -> m.getName().equals("apply"))
                .forEach(m -> analyzeCallback2(d1, incoming, stmt, m, manager, strategy));
    }

    private void analyzeCallback2(Abstraction d1, Abstraction incoming, Stmt stmt, SootMethod sm, InfoflowManager manager, IContainerStrategy strategy) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());

        if (!sm.hasActiveBody()) {
            synchronized (sm) {
                if (!sm.hasActiveBody()) {
                    sm.retrieveActiveBody();
                    manager.getICFG().notifyMethodChanged(sm);
                }
            }
        }

        Local p1 = sm.getActiveBody().getThisLocal();
        AccessPath thisAp = manager.getAccessPathFactory().copyWithNewValue(incoming.getAccessPath(), p1);
        Abstraction abs = new Abstraction(null, thisAp, null, null, false, false);


        Value base = iie.getBase();
        ContextDefinition[] ctxt = new ContextDefinition[keys.length];
        for (int i = 0; i < ctxt.length; i++) {
            if (keys[i].getParamIdx() == ParamIndex.LAST_INDEX.toInt())
                ctxt[i] = strategy.getNextPosition(base, stmt);
            else if (keys[i].isValueBased())
                ctxt[i] = strategy.getIndexContext(iie.getArg(keys[i].getParamIdx()), stmt);
            else
                ctxt[i] = strategy.getKeyContext(iie.getArg(keys[i].getParamIdx()), stmt);
        }
        if (strategy.shouldSmash(ctxt))
            ctxt = null;

        AccessPathFragment[] oldFragments = incoming.getAccessPath().getFragments();
        int len = oldFragments == null ? 0 : oldFragments.length;
        AccessPathFragment[] fragments = new AccessPathFragment[len];
        if (oldFragments != null && len > 1)
            System.arraycopy(oldFragments, 0, fragments, 2, len - 2);
        SootField f = safeGetField(field);
        fragments[0] = new AccessPathFragment(f, f.getType(), ctxt);
        AccessPath ap = manager.getAccessPathFactory().createAccessPath(base, fragments, incoming.getAccessPath().getTaintSubFields());
        Abstraction newAbs = incoming.deriveNewAbstraction(ap, stmt);

        CollectionTaintWrapper tw = (CollectionTaintWrapper) manager.getTaintWrapper();
        tw.registerCallback(d1, stmt, newAbs, abs);

        for (Unit sP : manager.getICFG().getStartPointsOf(sm)) {
            PathEdge<Unit, Abstraction> edge = new PathEdge<>(abs, sP, abs);
            manager.getMainSolver().processEdge(edge);
        }
    }

    private void analyzeCallback(Abstraction d1, Abstraction curr, Stmt stmt, SootMethod sm, InfoflowManager manager) {
        if (!sm.hasActiveBody()) {
            synchronized (sm) {
                if (!sm.hasActiveBody()) {
                    sm.retrieveActiveBody();
                    manager.getICFG().notifyMethodChanged(sm);
                }
            }
        }

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
