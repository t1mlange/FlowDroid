package soot.jimple.infoflow.collections.strategies.containers;

import java.util.concurrent.ConcurrentHashMap;

import soot.Local;
import soot.SootMethod;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.CollectionTaintWrapper;
import soot.jimple.infoflow.collections.analyses.ListSizeAnalysis;
import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.collections.context.KeySetContext;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

public class ConstantStrategy extends ConstantMapStrategy {
    private final ConcurrentHashMap<SootMethod, ListSizeAnalysis> implicitIndices;
    protected final InfoflowManager manager;

    public ConstantStrategy(InfoflowManager manager, CollectionTaintWrapper ctw) {
        super(ctw);
        this.implicitIndices = new ConcurrentHashMap<>();
        this.manager = manager;
    }

    @Override
    public Tristate intersect(ContextDefinition apKey, ContextDefinition stmtKey) {
        if (apKey == UnknownContext.v() || stmtKey == UnknownContext.v())
            return Tristate.MAYBE();

        if (apKey instanceof IntervalContext)
            return ((IntervalContext) apKey).intersect((IntervalContext) stmtKey);
        if (apKey instanceof KeySetContext)
            return ((KeySetContext) apKey).intersect((KeySetContext) stmtKey);

        throw new RuntimeException("Got unknown context: " + apKey.getClass());
    }

    @Override
    public ContextDefinition getIndexContext(Value value, Stmt stmt) {
        if (value instanceof IntConstant)
            return new IntervalContext(((IntConstant) value).value);

        return UnknownContext.v();
    }

    @Override
    public ContextDefinition getNextPosition(Value value, Stmt stmt) {
        return getContextFromImplicitKey(value, stmt, false);
    }

    @Override
    public ContextDefinition getFirstPosition(Value value, Stmt stmt) {
        return new IntervalContext(0);
    }

    @Override
    public ContextDefinition getLastPosition(Value value, Stmt stmt) {
        return getContextFromImplicitKey(value, stmt, true);
    }

    @Override
    public Tristate lessThanEqual(ContextDefinition ctxt1, ContextDefinition ctxt2) {
        if (ctxt1 == UnknownContext.v() || ctxt2 == UnknownContext.v())
            return Tristate.MAYBE();

        if (ctxt1 instanceof IntervalContext && ctxt2 instanceof IntervalContext)
            return ((IntervalContext) ctxt1).lessThanEqual(((IntervalContext) ctxt2));

        throw new RuntimeException("Unknown combination of " + ctxt1.toString() + " and " + ctxt2.toString());
    }

    @Override
    public ContextDefinition shiftRight(ContextDefinition ctxt, Stmt stmt, boolean exact) {
        if (ctxt instanceof IntervalContext)
            return exact ? ((IntervalContext) ctxt).shiftRight() : ((IntervalContext) ctxt).addRight();

        throw new RuntimeException("Expect interval context but got instead: " + ctxt);
    }

    @Override
    public ContextDefinition shiftLeft(ContextDefinition ctxt, Stmt stmt, boolean exact) {
        if (ctxt instanceof IntervalContext)
            return exact ? ((IntervalContext) ctxt).shiftLeft() : ((IntervalContext) ctxt).subtractLeft();

        throw new RuntimeException("Expect interval context but got instead: " + ctxt);
    }

    private ContextDefinition getContextFromImplicitKey(Value value, Stmt stmt, boolean decr) {
        if (value instanceof Local) {
            SootMethod currMethod = manager.getICFG().getMethodOf(stmt);
            var lstSizeAnalysis = implicitIndices.computeIfAbsent(currMethod,
                    sm -> new ListSizeAnalysis(manager.getICFG().getOrCreateUnitGraph(sm)));
            ListSizeAnalysis.ListSize size = lstSizeAnalysis.getFlowBefore(stmt).get(value);
            if (size != null && !size.isBottom()) {
                return new IntervalContext(decr ? size.getSize() - 1 : size.getSize());
            }
        }

        return UnknownContext.v();
    }
}
