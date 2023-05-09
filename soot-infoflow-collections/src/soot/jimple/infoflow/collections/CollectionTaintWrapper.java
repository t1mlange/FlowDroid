package soot.jimple.infoflow.collections;

import heros.DontSynchronize;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.CollectionMethod;
import soot.jimple.infoflow.collections.data.CollectionModel;
import soot.jimple.infoflow.collections.operations.ICollectionOperation;
import soot.jimple.infoflow.collections.strategies.ConstantKeyStrategy;
import soot.jimple.infoflow.collections.strategies.IKeyStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

import java.util.*;

public class CollectionTaintWrapper implements ITaintPropagationWrapper {

    @DontSynchronize("Read-only during analysis")
    private final Map<String, CollectionModel> models;

    @DontSynchronize("Read-only during analysis")
    private final ITaintPropagationWrapper fallbackWrapper;

    @DontSynchronize("Benign race")
    private int wrapperHits;
    @DontSynchronize("Benign race")
    private int wrapperMisses;

    private InfoflowManager manager;

    private IKeyStrategy strategy;

    public CollectionTaintWrapper(Map<String, CollectionModel> models,
                                  ITaintPropagationWrapper fallbackWrapper) {
        this.models = models;
        this.fallbackWrapper = fallbackWrapper;
        this.wrapperHits = 0;
        this.wrapperMisses = 0;
        this.strategy = new ConstantKeyStrategy();
    }

    @Override
    public void initialize(InfoflowManager manager) {
        this.manager = manager;

        if (fallbackWrapper != null)
            fallbackWrapper.initialize(manager);
    }

    @Override
    public Collection<PreAnalysisHandler> getPreAnalysisHandlers() {
        if (fallbackWrapper != null)
            return fallbackWrapper.getPreAnalysisHandlers();
        return Collections.emptyList();
    }

    private Set<Abstraction> fallbackTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
        return fallbackWrapper == null ? null : fallbackWrapper.getTaintsForMethod(stmt, d1, taintedPath);
    }

    @Override
    public Set<Abstraction> getTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
        if (!stmt.containsInvokeExpr())
            return fallbackTaintsForMethod(stmt, d1, taintedPath);

        CollectionMethod method = getCollectionMethodForSootMethod(stmt.getInvokeExpr().getMethod());
        if (method == null)
            return fallbackTaintsForMethod(stmt, d1, taintedPath);

        wrapperHits++;

        Set<Abstraction> outSet = new HashSet<>();
        boolean killCurrentTaint = false;
        for (ICollectionOperation op : method.operations()) {
            killCurrentTaint |= op.apply(stmt, taintedPath, outSet, manager, strategy);
        }

        if (!killCurrentTaint)
            outSet.add(taintedPath);

        return outSet;
    }

    @Override
    public boolean isExclusive(Stmt stmt, Abstraction taintedPath) {
        if (supportsCallee(stmt))
            return true;

        if (fallbackWrapper != null)
            return fallbackWrapper.isExclusive(stmt, taintedPath);
        return false;
    }

    @Override
    public Set<Abstraction> getAliasesForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
        if (fallbackWrapper != null)
            return fallbackWrapper.getAliasesForMethod(stmt, d1, taintedPath);
        return null;
    }

    private CollectionMethod getCollectionMethodForSootMethod(SootMethod sm) {
        CollectionModel model = models.get(sm.getDeclaringClass().getName());
        if (model != null) {
            return model.getMethod(sm.getSubSignature());
        }
        return null;
    }

    @Override
    public boolean supportsCallee(SootMethod sm) {
        if (getCollectionMethodForSootMethod(sm) != null)
            return true;

        return fallbackWrapper != null && fallbackWrapper.supportsCallee(sm);
    }

    @Override
    public boolean supportsCallee(Stmt callSite) {
        if (callSite.containsInvokeExpr())
            return supportsCallee(callSite.getInvokeExpr().getMethod());
        return fallbackWrapper != null && fallbackWrapper.supportsCallee(callSite);
    }

    @Override
    public int getWrapperHits() {
        return this.wrapperHits + fallbackWrapper.getWrapperHits();
    }

    @Override
    public int getWrapperMisses() {
        return this.wrapperMisses + fallbackWrapper.getWrapperMisses();
    }
}
