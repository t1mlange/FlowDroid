package soot.jimple.infoflow.collections.strategies.containers;

import java.util.concurrent.ConcurrentHashMap;

import soot.Local;
import soot.SootMethod;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.analyses.ListSizeAnalysis;
import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.collections.context.KeySetContext;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.strategies.containers.shift.IShiftOperation;
import soot.jimple.infoflow.collections.strategies.containers.shift.PreciseShift;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

/**
 * Strategy that reasons about maps with constant keys and lists with constant indices.
 * Uses {@link ListSizeAnalysis} to retrieve the list size, thus, should only be used for testing
 * the semantics of list operations.
 *
 * @author Tim Lange
 */
public class TestConstantStrategy extends AbstractListStrategy {
    private final ConcurrentHashMap<SootMethod, ListSizeAnalysis> implicitIndices;

    public TestConstantStrategy(InfoflowManager manager) {
        super(manager);
        this.implicitIndices = new ConcurrentHashMap<>();
    }

    public TestConstantStrategy(InfoflowManager manager, IShiftOperation shiftOp) {
        super(manager, shiftOp);
        this.implicitIndices = new ConcurrentHashMap<>();
    }

    @Override
    public ContextDefinition getNextPosition(Value value, Stmt stmt) {
        if (!shouldResolveIndex(value))
            return UnknownContext.v();

        return getContextFromImplicitKey(value, stmt, false);
    }

    @Override
    public ContextDefinition getLastPosition(Value value, Stmt stmt) {
        if (!shouldResolveIndex(value))
            return UnknownContext.v();

        return getContextFromImplicitKey(value, stmt, true);
    }

    private ContextDefinition getContextFromImplicitKey(Value value, Stmt stmt, boolean decr) {
        if (value instanceof Local) {
            SootMethod currMethod = manager.getICFG().getMethodOf(stmt);
            var lstSizeAnalysis = implicitIndices.computeIfAbsent(currMethod,
                    sm -> new ListSizeAnalysis(manager.getICFG().getOrCreateUnitGraph(sm)));
            ListSizeAnalysis.ListSize size = lstSizeAnalysis.getFlowBefore(stmt).get(value);
            if (size != null && !size.isBottom()) {
                resolvedIndices++;
                return new IntervalContext(decr ? size.getSize() - 1 : size.getSize());
            }
        }

        unresolvedIndices++;
        return UnknownContext.v();
    }
}
