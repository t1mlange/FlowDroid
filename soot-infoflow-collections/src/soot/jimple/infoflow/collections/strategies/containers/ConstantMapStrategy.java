package soot.jimple.infoflow.collections.strategies.containers;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import soot.Local;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.CollectionTaintWrapper;
import soot.jimple.infoflow.collections.analyses.IntraproceduralListSizeAnalysis;
import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.collections.context.KeySetContext;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.data.CollectionMethod;
import soot.jimple.infoflow.collections.data.CollectionModel;
import soot.jimple.infoflow.collections.operations.LocationDependentOperation;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

public class ConstantMapStrategy implements IContainerStrategy {
    private final CollectionTaintWrapper ctw;

    public ConstantMapStrategy(CollectionTaintWrapper ctw) {
        this.ctw = ctw;
    }

    @Override
    public Tristate intersect(ContextDefinition apKey, ContextDefinition stmtKey) {
        if (apKey == UnknownContext.v() || stmtKey == UnknownContext.v())
            return Tristate.MAYBE();

        if (apKey instanceof KeySetContext)
            return ((KeySetContext) apKey).intersect((KeySetContext) stmtKey);

        throw new RuntimeException("Got unknown context: " + apKey.getClass());
    }

    @Override
    public ContextDefinition getKeyContext(Value value, Stmt stmt) {
        if (value instanceof Constant)
            return new KeySetContext((Constant) value);

        return UnknownContext.v();
    }

    @Override
    public ContextDefinition getIndexContext(Value value, Stmt stmt) {
        return UnknownContext.v();
    }

    @Override
    public ContextDefinition getNextPosition(Value value, Stmt stmt) {
        return UnknownContext.v();
    }

    @Override
    public ContextDefinition getFirstPosition(Value value, Stmt stmt) {
        return UnknownContext.v();
    }

    @Override
    public ContextDefinition getLastPosition(Value value, Stmt stmt) {
        return UnknownContext.v();
    }

    @Override
    public Tristate lessThanEqual(ContextDefinition ctxt1, ContextDefinition ctxt2) {
        return Tristate.MAYBE();
    }

    @Override
    public ContextDefinition shiftRight(ContextDefinition ctxt, Stmt stmt, boolean exact) {
        return UnknownContext.v();
    }

    @Override
    public ContextDefinition shiftLeft(ContextDefinition ctxt, Stmt stmt, boolean exact) {
        return UnknownContext.v();
    }

    @Override
    public boolean shouldSmash(ContextDefinition[] ctxts) {
        for (ContextDefinition ctxt : ctxts) {
            if (ctxt.containsInformation())
                return false;
        }

        return true;
    }

    @Override
    public boolean stmtDependsOnContext(Stmt stmt) {
        if (!stmt.containsInvokeExpr())
            return false;
        CollectionMethod cm = ctw.getCollectionMethodForSootMethod(stmt.getInvokeExpr().getMethod());
        if (cm == null)
            return false;

        return Arrays.stream(cm.operations())
                .filter(op -> op instanceof LocationDependentOperation)
                .map(op -> ((LocationDependentOperation) op).buildContext(this,
                        (InstanceInvokeExpr) stmt.getInvokeExpr(), stmt))
                .anyMatch(ctxt -> ctxt != null && !shouldSmash(ctxt));
    }
}
