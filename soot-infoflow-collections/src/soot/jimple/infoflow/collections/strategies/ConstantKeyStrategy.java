package soot.jimple.infoflow.collections.strategies;

import soot.Local;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.IntConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.analyses.IntraproceduralListSizeAnalysis;
import soot.jimple.infoflow.collections.context.ConstantContext;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.context.WildcardContext;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.concurrent.ConcurrentHashMap;

public class ConstantKeyStrategy implements IContainerStrategy {
    private final ConcurrentHashMap<SootMethod, IntraproceduralListSizeAnalysis> implicitIndices;
    private final InfoflowManager manager;

    public ConstantKeyStrategy(InfoflowManager manager) {
        this.implicitIndices = new ConcurrentHashMap<>();
        this.manager = manager;
    }

    @Override
    public Tristate intersect(ContextDefinition apKey, ContextDefinition stmtKey) {
        if (stmtKey == UnknownContext.v())
            return Tristate.MAYBE();

        if (stmtKey == WildcardContext.v())
            return Tristate.TRUE();

        if (((ConstantContext) apKey).getConstant().equals(((ConstantContext) stmtKey).getConstant()))
            return Tristate.TRUE();
        return Tristate.FALSE();
    }

    @Override
    public ContextDefinition getContextFromKey(Value value, Stmt stmt) {
        if (value instanceof Constant)
            return new ConstantContext((Constant) value);

        return UnknownContext.v();
    }

    @Override
    public ContextDefinition getNextPosition(Value value, Stmt stmt) {
        return getContextFromImplicitKey(value, stmt, false);
    }

    @Override
    public ContextDefinition getLastPosition(Value value, Stmt stmt) {
        return getContextFromImplicitKey(value, stmt, true);
    }

    @Override
    public Tristate lessThan(ContextDefinition ctxt1, ContextDefinition ctxt2) {
        if (ctxt1 instanceof ConstantContext && ctxt2 instanceof ConstantContext) {
            Constant c1 = ((ConstantContext) ctxt1).getConstant();
            Constant c2 = ((ConstantContext) ctxt2).getConstant();
            return Tristate.fromBoolean(((IntConstant) c1).value < ((IntConstant) c2).value);
        }

        if (ctxt2 == UnknownContext.v())
            return Tristate.MAYBE();

        throw new RuntimeException("Unknown combination of " + ctxt1.toString() + " and " + ctxt2.toString());
    }

    @Override
    public ContextDefinition shiftRight(ContextDefinition ctxt) {
        if (ctxt instanceof ConstantContext) {
            IntConstant c = (IntConstant) ((ConstantContext) ctxt).getConstant();
            return new ConstantContext(IntConstant.v(c.value+1));
        }

        throw new RuntimeException("Unexpected context: " + ctxt);
    }

    private ContextDefinition getContextFromImplicitKey(Value value, Stmt stmt, boolean decr) {
        if (value instanceof Local) {
            SootMethod currMethod = manager.getICFG().getMethodOf(stmt);
            var lstSizeAnalysis = implicitIndices.computeIfAbsent(currMethod,
                    sm -> new IntraproceduralListSizeAnalysis(manager.getICFG().getOrCreateUnitGraph(sm)));
            Integer i = lstSizeAnalysis.getFlowBefore(stmt).get(value);
            if (i != null)
                return new ConstantContext(IntConstant.v(decr ? i-1 : i));
        }

        return UnknownContext.v();
    }

    @Override
    public boolean shouldSmash(ContextDefinition[] ctxts) {
        for (ContextDefinition ctxt : ctxts) {
            if (ctxt != UnknownContext.v())
                return false;
        }

        return true;
    }
}
