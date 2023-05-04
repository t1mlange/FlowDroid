package soot.jimple.infoflow.collections;

import soot.SootMethod;
import soot.Type;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

import java.util.Set;

public class CollectionTaintWrapper implements ITaintPropagationWrapper {
    private final ITaintPropagationWrapper fallbackWrapper;

    private int wrapperHits;
    private int wrapperMisses;

    public CollectionTaintWrapper(ITaintPropagationWrapper fallbackWrapper) {
        this.fallbackWrapper = fallbackWrapper;
        this.wrapperHits = 0;
        this.wrapperMisses = 0;
    }

    @Override
    public void initialize(InfoflowManager manager) {
        fallbackWrapper.initialize(manager);
    }

    @Override
    public Set<Abstraction> getTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {

        if (fallbackWrapper != null)
            return getTaintsForMethod(stmt, d1, taintedPath);
        return null;
    }

    @Override
    public boolean isExclusive(Stmt stmt, Abstraction taintedPath) {
        if (fallbackWrapper != null)
            return isExclusive(stmt, taintedPath);
        return false;
    }

    @Override
    public Set<Abstraction> getAliasesForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
        if (fallbackWrapper != null)
            return getAliasesForMethod(stmt, d1, taintedPath);
        return null;
    }

    @Override
    public boolean supportsCallee(SootMethod method) {
        if (fallbackWrapper != null)
            return supportsCallee(method);
        return false;
    }

    @Override
    public boolean supportsCallee(Stmt callSite) {
        if (fallbackWrapper != null)
            return supportsCallee(callSite);
        return false;
    }

    @Override
    public int getWrapperHits() {
        return this.wrapperHits + fallbackWrapper.getWrapperHits();
    }

    @Override
    public int getWrapperMisses() {
        return this.wrapperMisses + fallbackWrapper.getWrapperMisses();
    }

    @Override
    public boolean isSubType(Type t1, Type t2) {
        return false;
    }
}
