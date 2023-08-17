package soot.jimple.infoflow.collections.analyses;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import heros.solver.IDESolver;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Intraprocedural analysis that reasons whether an iterator mutates the underlying collection or not
 *
 * @author Tim Lange
 */
public class ROIteratorAnalysis {
    private final LoadingCache<UnitGraph, SimpleLocalUses> graphToUses= IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<>() {
        @Override
        public SimpleLocalUses load(UnitGraph ug) throws Exception {
            return new SimpleLocalUses(ug, new SimpleLocalDefs(ug));
        }
    });

    private final Set<String> readOperations;
    private final FastHierarchy fh;
    private final SootClass iteratorClass;
    private final IInfoflowCFG icfg;

    public ROIteratorAnalysis(IInfoflowCFG icfg) throws IllegalStateException {
        this.icfg = icfg;

        this.readOperations = new HashSet<>();
        readOperations.add("void forEachRemaining(java.util.Consumer)");
        readOperations.add("boolean hasNext()");
        readOperations.add("boolean hasPrevious()");
        readOperations.add("java.lang.Object next()");
        readOperations.add("int nextIndex()");
        readOperations.add("java.lang.Object previous()");
        readOperations.add("int previousIndex()");
        readOperations.add("boolean equals(java.lang.Object)");
        readOperations.add("int hashCode()");

        this.fh = Scene.v().getOrMakeFastHierarchy();
        this.iteratorClass = Scene.v().getSootClassUnsafe("java.util.Iterator");
        if (this.iteratorClass == null)
            throw new IllegalStateException("Soot is not yet loaded!");
    }

    /**
     * Returns whether the given method is altering an iterator or an unknown method
     *
     * @param sm method
     * @return true if the given method is an iterator method and not in the whitelist of read operations
     */
    private boolean isWriteOrUnknownOperation(SootMethod sm) {
        return fh.canStoreClass(sm.getDeclaringClass(), iteratorClass)
                && !readOperations.contains(sm.getSubSignature());
    }

    /**
     * Checks whether this assignment creates an iterator that is only used to read values
     * but never mutates the underlying collection
     *
     * @param unit current unit
     * @return true if iterator is read-only
     */
    public boolean isReadOnlyIterator(Unit unit) {
        // If this unit is no assign statement, we can't handle it
        // and assume it might be mutating the collection
        if (!(unit instanceof AssignStmt))
            return false;

        UnitGraph ug = (UnitGraph) icfg.getOrCreateUnitGraph(icfg.getMethodOf(unit));
        return isReadOnlyIteratorInternal(graphToUses.getUnchecked(ug), (AssignStmt) unit);
    }

    private boolean isReadOnlyIteratorInternal(SimpleLocalUses du, AssignStmt assign) {
        // Check that we actually have created an iterator
        Value lhs = assign.getLeftOp();
        // The DU analysis of Soot does not support field (for now)
        if (!(lhs instanceof Local))
            return false;
        Type lhsType = lhs.getType();
        if (!(lhsType instanceof RefType)
                || !fh.canStoreClass(((RefType) lhsType).getSootClass(), iteratorClass))
            return false;

        List<UnitValueBoxPair> usesList = du.getUsesOf(assign);
        // For each use of a local
        for (UnitValueBoxPair uv : usesList) {
            Stmt stmt = (Stmt) uv.getUnit();
            // We assume an iterator also mutates the collection if
            // 1. a method is called on it that is unknown
            // 2. it is leaving the method as an argument
            // 3. the iterator leaves the method through a return statement
            // 4. is assigned to a field
            if (stmt.containsInvokeExpr()) {
                if (stmt.getInvokeExpr().getArgs().contains(lhs))
                    return false;
                if (isWriteOrUnknownOperation(stmt.getInvokeExpr().getMethod()))
                    return false;
            } else if (stmt instanceof AssignStmt) {
                // All e = it.next() will be caught by the previous case s.t.
                // here we should only see assignments or casts
                if (!isReadOnlyIteratorInternal(du, (AssignStmt) stmt))
                    return false;
            } else if (stmt instanceof ReturnStmt) {
                return false;
            }
        }

        // If we have seen all statements, we can assume the iterator is read-only
        return true;
    }
}
