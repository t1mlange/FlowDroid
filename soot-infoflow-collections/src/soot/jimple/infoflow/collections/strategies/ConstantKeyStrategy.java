package soot.jimple.infoflow.collections.strategies;

import soot.*;
import soot.jimple.Constant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.analyses.IntraproceduralListSizeAnalysis;
import soot.jimple.infoflow.collections.context.ConstantContext;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.context.WildcardContext;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.util.Cons;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConstantKeyStrategy implements IContainerStrategy {
    private final ConcurrentHashMap<SootMethod, IntraproceduralListSizeAnalysis> implicitIndices;
    private final InfoflowManager manager;

    public ConstantKeyStrategy(InfoflowManager manager) {
        this.implicitIndices = new ConcurrentHashMap<>();
        this.manager = manager;
    }

    private <T> boolean setIntersect(Set<T> s1, Set<T> s2) {
        for (T c : s1) {
            if (s2.contains(c))
                return true;
        }
        return false;
    }

    @Override
    public Tristate intersect(ContextDefinition apKey, ContextDefinition stmtKey) {
        if (stmtKey == UnknownContext.v())
            return Tristate.MAYBE();

        if (stmtKey == WildcardContext.v())
            return Tristate.TRUE();

        Set<Constant> apC = ((ConstantContext) apKey).getConstants();
        Set<Constant> stmtC = ((ConstantContext) stmtKey).getConstants();
        if (apC.equals(stmtC))
            return Tristate.TRUE();

        if (setIntersect(apC, stmtC))
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

    private <T> T max(Set<T> s, BiFunction<T, T, Boolean> ltComparator) {
        T max = null;
        for (T e : s) {
            if (max == null || ltComparator.apply(max, e))
                max = e;
        }
        return max;
    }

    private <T> T min(Set<T> s, BiFunction<T, T, Boolean> ltComparator) {
        T min = null;
        for (T e : s) {
            if (min == null || ltComparator.apply(e, min))
                min = e;
        }
        return min;
    }

    private <T> boolean lessThan(Set<T> s1, Set<T> s2,
                               BiFunction<T, T, Boolean> ltComparator) {
        return ltComparator.apply(max(s1, ltComparator), min(s2, ltComparator));
    }

    @Override
    public Tristate lessThan(ContextDefinition ctxt1, ContextDefinition ctxt2) {
        if (ctxt1 instanceof ConstantContext && ctxt2 instanceof ConstantContext) {
            Set<Constant> c1 = ((ConstantContext) ctxt1).getConstants();
            Set<Constant> c2 = ((ConstantContext) ctxt2).getConstants();
            return Tristate.fromBoolean(lessThan(c1, c2, (a, b) -> ((IntConstant) a).value < ((IntConstant) b).value));
        }

        if (ctxt2 == UnknownContext.v())
            return Tristate.MAYBE();

        throw new RuntimeException("Unknown combination of " + ctxt1.toString() + " and " + ctxt2.toString());
    }

    @Override
    public ContextDefinition shiftRight(ContextDefinition ctxt) {
        if (ctxt instanceof ConstantContext) {
            Set<Constant> s = ((ConstantContext) ctxt).getConstants();
            return new ConstantContext(s.stream().map(c -> IntConstant.v(((IntConstant) c).value + 1)).collect(Collectors.toSet()));
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
            if (ctxt instanceof ConstantContext) {
                if (((ConstantContext) ctxt).getConstants().size() > 5)
                    return true;
            }
            if (ctxt != UnknownContext.v())
                return false;
        }

        return true;
    }
}
