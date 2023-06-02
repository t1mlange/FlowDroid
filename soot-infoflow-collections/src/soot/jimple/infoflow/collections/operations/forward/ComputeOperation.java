package soot.jimple.infoflow.collections.operations.forward;

import heros.solver.PathEdge;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.CollectionTaintWrapper;
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

public class ComputeOperation extends LocationDependentOperation {
    private final int data; // Index of data provided to a map
    private final int callbackIdx; // Index of the callback parameter
    private final int callbackBaseIdx; // Index of the contents parameter in the apply() function
    private final int callbackDataIdx; // Index of the value parameter in the apply() function
    private final boolean doReturn;

    public ComputeOperation(Location[] keys, String field, int data,
                            int callbackIdx, int callbackBaseIdx, int callbackDataIdx,
                            boolean doReturn) {
        super(keys, field);
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

        // Callback computes something based on the contents of the collection
        // i.e. Map.computeIfPresent()
        boolean usesContents = callbackBaseIdx != ParamIndex.UNUSED;
        if (usesContents
                && manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getBase()))
            return identityDependsOnCallback(d1, incoming, stmt, manager, strategy);

        // Callback takes a value parameter from the original method call, i.e. Map.computeIfAbsent() or Map.merge()
        if (data != ParamIndex.UNUSED
                && manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getArg(data))) {
            manager.getICFG().getCalleesOfCallAt(stmt).stream()
                    .filter(m -> m.getName().equals("apply"))
                    .forEach(m -> analyzeCallbackWithTaintedData(d1, incoming, stmt, m, manager, strategy));
            return false;
        }

        // Callback function argument is tainted
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

        Tristate state = matchContexts(fragment, iie, stmt, strategy, null);

        // If we are sure the collection is not tainted here, we can skip the callback handling
        if (state.isFalse())
            return false;

        // Analyze each callback present in the call-graph
        manager.getICFG().getCalleesOfCallAt(stmt).stream()
                .filter(m -> m.getName().equals("apply"))
                .forEach(m -> analyzeCallbackWithTaintedCollection(d1, incoming, stmt, m, strategy, manager));

        return true;
    }

    private void analyzeCallbackWithTaintedData(Abstraction d1, Abstraction incoming, Stmt stmt, SootMethod sm, InfoflowManager manager, IContainerStrategy strategy) {
        maybeInitMethodBody(sm, manager);

        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());

        ContextDefinition[] ctxt = buildContext(strategy, iie, stmt);
        AccessPath ap = taintCollectionWithContext(iie.getBase(), ctxt, incoming.getAccessPath(), manager);
        if (ap == null)
            return;
        Abstraction newAbs = incoming.deriveNewAbstraction(ap, stmt);

        Abstraction abs = createParameterLocalTaint(sm, incoming, this.callbackDataIdx, manager);
        process(sm, d1, stmt, newAbs, abs, strategy, manager);
    }

    private void analyzeCallbackWithTaintedCallback(Abstraction d1, Abstraction incoming, Stmt stmt, SootMethod sm, InfoflowManager manager, IContainerStrategy strategy) {
        maybeInitMethodBody(sm, manager);

        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        ContextDefinition[] ctxt = buildContext(strategy, iie, stmt);
        AccessPath ap = taintCollectionWithContext(iie.getBase(), ctxt, AccessPath.getEmptyAccessPath(), manager);
        if (ap == null)
            return;
        Abstraction newAbs = incoming.deriveNewAbstraction(ap, stmt);

        Abstraction abs = createThisLocalTaint(sm, incoming, manager);
        process(sm, d1, stmt, newAbs, abs, strategy, manager);
    }

    private void analyzeCallbackWithTaintedCollection(Abstraction d1, Abstraction incoming, Stmt stmt, SootMethod sm,
                                                      IContainerStrategy strategy, InfoflowManager manager) {
        maybeInitMethodBody(sm, manager);
        Abstraction abs = createParameterLocalTaint(sm, incoming, this.callbackBaseIdx, manager);
        process(sm, d1, stmt, incoming, abs, strategy, manager);
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

    private void process(SootMethod sm, Abstraction d1, Stmt stmt, Abstraction injectIfSuccessful, Abstraction abs,
                         IContainerStrategy strategy, InfoflowManager manager) {
        CollectionTaintWrapper tw = (CollectionTaintWrapper) manager.getTaintWrapper();
        tw.registerCallback(d1, stmt, injectIfSuccessful, abs, sm);
        if (doReturn && stmt instanceof AssignStmt) {
            AccessPath ap = taintReturnValue(((AssignStmt) stmt).getLeftOp(), null, null,
                    injectIfSuccessful, strategy, manager);
            Abstraction retAbs = injectIfSuccessful.deriveNewAbstraction(ap, stmt);
            if (retAbs != null)
                tw.registerCallback(d1, stmt, retAbs, abs, sm);
        }

        for (Unit sP : manager.getICFG().getStartPointsOf(sm)) {
            PathEdge<Unit, Abstraction> edge = new PathEdge<>(abs, sP, abs);
            manager.getMainSolver().processEdge(edge);
        }
    }
}
