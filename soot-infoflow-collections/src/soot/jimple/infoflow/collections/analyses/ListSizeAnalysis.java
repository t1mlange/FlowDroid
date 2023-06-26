package soot.jimple.infoflow.collections.analyses;

import java.util.*;

import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

/**
 * Simple intraprocedural list size analysis performing constant propagation on the list size
 *
 * @author Tim Lange
 */
public class ListSizeAnalysis extends ForwardFlowAnalysis<Unit, Map<Local, ListSizeAnalysis.ListSize>> {

    /**
     *  NOT YET INITIALIZED OR NO LIST -> implicitly null
     *    / /  /      \
     *   0  1  2  ...  n
     *   \  \  \      /
     *  NON-CONSTANT SIZE -> BOTTOM
     */
    public static class ListSize {
        int size;
        boolean isBottom;

        ListSize(int size) {
            this.size = size;
        }

        static ListSize bottom() {
            return new ListSize();
        }

        private ListSize() {
            isBottom = true;
        }

        ListSize plusOne() {
            if (isBottom)
                return this;
            return new ListSize(size + 1);
        }

        ListSize minusOne() {
            if (isBottom)
                return this;
            return new ListSize(size - 1);
        }

        public int getSize() {
            return size;
        }

        public boolean isBottom() {
            return isBottom;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ListSize other = (ListSize) o;
            return size == other.size && isBottom == other.isBottom;
        }

        @Override
        public int hashCode() {
            return Objects.hash(size, isBottom);
        }
    }

    private final SootClass listClass;

    private static final Set<String> incrementors = new HashSet<>();
    static {
        incrementors.add("boolean add(java.lang.Object)");
        incrementors.add("java.lang.Object push(java.lang.Object)");
        incrementors.add("void addElement(java.lang.Object)");
    }

    public ListSizeAnalysis(DirectedGraph<Unit> graph) {
        super(graph);
        listClass = Scene.v().getSootClassUnsafe("java.util.List");
        doAnalysis();
    }

    @Override
    protected void flowThrough(Map<Local, ListSize> in, Unit unit, Map<Local, ListSize> out) {
        out.putAll(in);
        Stmt stmt = (Stmt) unit;
        if (stmt instanceof AssignStmt) {
            Value leftOp = ((AssignStmt) stmt).getLeftOp();
            Value rightOp = ((AssignStmt) stmt).getRightOp();

            if (leftOp instanceof Local && rightOp instanceof NewExpr) {
                SootClass sc = ((NewExpr) rightOp).getBaseType().getSootClass();
                if (Scene.v().getFastHierarchy().getAllImplementersOfInterface(listClass).contains(sc)) {
                    out.put((Local) leftOp, new ListSize(0));
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

        if (incrementors.contains(sm.getSubSignature())) {
            Local base = (Local) ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase();
            ListSize size = out.get(base);
            if (size != null)
                out.put(base, size.plusOne());
        } else if (sm.getSubSignature().equals("java.lang.Object pop()")) {
            Local base = (Local) ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase();
            ListSize size = out.get(base);
            if (size != null)
                out.put(base, size.minusOne());
        }
    }

    @Override
    protected Map<Local, ListSize> newInitialFlow() {
        return new HashMap<>();
    }

    @Override
    protected void merge(Map<Local, ListSize> in1, Map<Local, ListSize> in2, Map<Local, ListSize> out) {
        // Must
        for (Local local : in1.keySet()) {
            ListSize in1Const = in1.get(local);
            ListSize in2Const = in1.get(local);
            if (in1Const == null)
                out.put(local, in2Const);
            else if (in2Const == null || in1Const.equals(in2Const))
                out.put(local, in1Const);
            else
                out.put(local, ListSize.bottom());
        }
    }

    @Override
    protected void copy(Map<Local, ListSize> source, Map<Local, ListSize> dest) {
        if (source == dest) {
            return;
        }
        dest.putAll(source);
    }
}
