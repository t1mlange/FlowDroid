package soot.jimple.infoflow.collections.analyses;

import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.HashMap;
import java.util.Map;

/**
 *  NOT YET INITIALIZED
 *    / /  /      \
 *   0  1  2  ...  n
 *   \  \  \      /
 *  NON-CONSTANT SIZE
 */
public class IntraproceduralListSizeAnalysis extends ForwardFlowAnalysis<Unit, Map<Local, Integer>> {
    private final SootClass listClass;

    public IntraproceduralListSizeAnalysis(DirectedGraph<Unit> graph) {
        super(graph);
        listClass = Scene.v().getSootClassUnsafe("java.util.List");
        doAnalysis();
    }

    @Override
    protected void flowThrough(Map<Local, Integer> in, Unit unit, Map<Local, Integer> out) {
        out.putAll(in);
        Stmt stmt = (Stmt) unit;
        if (stmt instanceof AssignStmt) {
            Value leftOp = ((AssignStmt) stmt).getLeftOp();
            Value rightOp = ((AssignStmt) stmt).getRightOp();

            if (leftOp instanceof Local && rightOp instanceof NewExpr) {
                SootClass sc = ((NewExpr) rightOp).getBaseType().getSootClass();
                if (Scene.v().getFastHierarchy().getAllImplementersOfInterface(listClass).contains(sc)) {
                    out.put((Local) leftOp, 0);
                }
            } else {
                if (in.containsKey(leftOp))
                    out.remove(leftOp);
            }

            // Invalidate list size if an alias is created
            if (in.containsKey(rightOp))
                 out.remove(rightOp);
        }

        if (!stmt.containsInvokeExpr())
            return;

        // Also invalidate list size if it flows into a callee
        for (Value v : stmt.getInvokeExpr().getArgs()) {
            if (in.containsKey(v))
                out.remove(v);
        }

        SootMethod sm = stmt.getInvokeExpr().getMethod();
        if (listClass != sm.getDeclaringClass()
                && !Scene.v().getFastHierarchy().getAllImplementersOfInterface(listClass).contains(sm.getDeclaringClass()))
            return;

        if (sm.getSubSignature().equals("boolean add(java.lang.Object)")
                || sm.getSubSignature().equals("java.lang.Object push(java.lang.Object)")) {
            Local base = (Local) ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase();
            Integer c = out.get(base);
            if (c != null)
                out.put(base, c+1);
        } else if (sm.getSubSignature().equals("java.lang.Object pop()")) {
            Local base = (Local) ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase();
            Integer c = out.get(base);
            if (c != null)
                out.put(base, c-1);
        }
    }

    @Override
    protected Map<Local, Integer> newInitialFlow() {
        return new HashMap<>();
    }

    @Override
    protected void merge(Map<Local, Integer> in1, Map<Local, Integer> in2, Map<Local, Integer> out) {
        // Must
        for (Local local : in1.keySet()) {
            Integer in1Const = in1.get(local);
            Integer in2Const = in1.get(local);
            if (in1Const != null && in1Const.equals(in2Const))
                out.put(local, in1Const);
        }
    }

    @Override
    protected void copy(Map<Local, Integer> source, Map<Local, Integer> dest) {
        if (source == dest) {
            return;
        }
        dest.putAll(source);
    }
}
