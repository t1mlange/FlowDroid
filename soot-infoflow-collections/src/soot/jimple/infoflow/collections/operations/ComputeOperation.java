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
    private final int data; // Index of data provided to a map
    private final int callbackIdx; // Index of the callback parameter
    private final int callbackBaseIdx; // Index of the value parameter in the apply() function
    private final int callbackDataIdx; // Index of the value parameter in the apply() function
    private final boolean doReturn;

    public ComputeOperation(Location[] keys, String field, String fieldType, int data,
                            int callbackIdx, int callbackBaseIdx, int callbackDataIdx,
                            boolean doReturn) {
        super(keys, field, fieldType);
        this.data = data;
        this.callbackIdx = callbackIdx;
        this.callbackBaseIdx = callbackBaseIdx;
        this.callbackDataIdx = callbackDataIdx;
        this.doReturn = doReturn;
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager,
                         IContainerStrategy strategy, Collection<Abstraction> out) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());

        // Some methods such as computeIfAbsent won't have a value provided
        if (callbackBaseIdx != ParamIndex.UNUSED.toInt()
                && manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getBase()))
            return identityDependsOnCallback(d1, incoming, stmt, manager, strategy);

        if (data != ParamIndex.UNUSED.toInt()
                && manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getArg(data))) {
            manager.getICFG().getCalleesOfCallAt(stmt).stream()
                    .filter(m -> m.getName().equals("apply"))
                    .forEach(m -> analyzeCallbackWithTaintedData(d1, incoming, stmt, m, manager, strategy));
            return false;
        }

        if (manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getArg(callbackIdx))) {
            manager.getICFG().getCalleesOfCallAt(stmt).stream()
                    .filter(m -> m.getName().equals("apply"))
                    .forEach(m -> analyzeCallbackWithTaintedCallback(d1, incoming, stmt, m, manager, strategy));
            return false;
        }

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
                // ALL always matches the context
                if (keys[i].getParamIdx() == ParamIndex.ALL.toInt())
                    continue;

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
                .forEach(m -> analyzeCallbackWithTaintedCollection(d1, incoming, stmt, m, manager));

        return true;
    }

    private void analyzeCallbackWithTaintedData(Abstraction d1, Abstraction incoming, Stmt stmt, SootMethod sm, InfoflowManager manager, IContainerStrategy strategy) {
        maybeInitMethodBody(sm, manager);

        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());

        Abstraction newAbs = createCollectionTaint(incoming, stmt, iie, manager, strategy);
        Abstraction abs = createParameterLocalTaint(sm, incoming, this.callbackDataIdx, manager);
        process(sm, d1, stmt, newAbs, abs, manager);
    }

    private void analyzeCallbackWithTaintedCallback(Abstraction d1, Abstraction incoming, Stmt stmt, SootMethod sm, InfoflowManager manager, IContainerStrategy strategy) {
        maybeInitMethodBody(sm, manager);

        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());

        Abstraction newAbs = createCollectionTaint(incoming, stmt, iie, manager, strategy);
        Abstraction abs = createThisLocalTaint(sm, incoming, manager);
        process(sm, d1, stmt, newAbs, abs, manager);
    }

    private void analyzeCallbackWithTaintedCollection(Abstraction d1, Abstraction incoming, Stmt stmt, SootMethod sm, InfoflowManager manager) {
        maybeInitMethodBody(sm, manager);
        Abstraction abs = createParameterLocalTaint(sm, incoming, this.callbackBaseIdx, manager);
        process(sm, d1, stmt, incoming, abs, manager);
    }

    private void maybeInitMethodBody(SootMethod sm, InfoflowManager manager) {
        if (!sm.hasActiveBody()) {
            synchronized (sm) {
                if (!sm.hasActiveBody()) {
                    sm.retrieveActiveBody();
                    manager.getICFG().notifyMethodChanged(sm);
                }
            }
        }
    }

    private Abstraction createCollectionTaint(Abstraction incoming, Stmt stmt, InstanceInvokeExpr iie,
                                              InfoflowManager manager, IContainerStrategy strategy) {
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
        int len = oldFragments == null ? 1: oldFragments.length;
        AccessPathFragment[] fragments = new AccessPathFragment[len];
        if (oldFragments != null && len > 1)
            System.arraycopy(oldFragments, 0, fragments, 1, len - 1);
        SootField f = safeGetField(field);
        fragments[0] = new AccessPathFragment(f, f.getType(), ctxt);
        AccessPath ap = manager.getAccessPathFactory().createAccessPath(base, fragments, incoming.getAccessPath().getTaintSubFields());
        return incoming.deriveNewAbstraction(ap, stmt);
    }

    private Abstraction createParameterLocalTaint(SootMethod sm, Abstraction incoming, int idx, InfoflowManager manager) {
        Local paramLocal = sm.getActiveBody().getParameterLocal(idx);
        AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(incoming.getAccessPath(), paramLocal,
                paramLocal.getType(), true);
        return new Abstraction(null, ap, null, null, false, false);
    }

    private Abstraction createThisLocalTaint(SootMethod sm, Abstraction incoming, InfoflowManager manager) {
        Local thisLocal = sm.getActiveBody().getThisLocal();
        AccessPath thisAp = manager.getAccessPathFactory().copyWithNewValue(incoming.getAccessPath(), thisLocal);
        return new Abstraction(null, thisAp, null, null, false, false);
    }

    private void process(SootMethod sm, Abstraction d1, Stmt stmt, Abstraction injectIfSuccessful, Abstraction abs, InfoflowManager manager) {
        CollectionTaintWrapper tw = (CollectionTaintWrapper) manager.getTaintWrapper();
        tw.registerCallback(d1, stmt, injectIfSuccessful, abs);

        for (Unit sP : manager.getICFG().getStartPointsOf(sm)) {
            PathEdge<Unit, Abstraction> edge = new PathEdge<>(abs, sP, abs);
            manager.getMainSolver().processEdge(edge);
        }
    }
}
