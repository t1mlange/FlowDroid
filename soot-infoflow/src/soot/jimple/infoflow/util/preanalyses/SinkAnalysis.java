package soot.jimple.infoflow.util.preanalyses;

import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.TypeUtils;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;

/**
 * Intraprocedural backward taint analysis without access paths using EasyTaintWrapper alike rules.
 * Intentionally imprecise and unsound. Idea: Find out for which direction the precise analysis performs better.
 *
 * @author Tim Lange
 */
public class SinkAnalysis extends BackwardFlowAnalysis<Unit, FlowSet<Local>> {
    SootMethod method;
    Stmt sink;
    int flowsIntoCallee = 0;
    int flowsIntoCaller = 0;
    int propagations = 0;
    int runtime;
    long timeBefore;

    public SinkAnalysis(DirectedGraph<Unit> graph, SootMethod method, Stmt sink) {
        super(graph);
        this.doAnalysis();
        this.method = method;
        this.sink = sink;
        this.timeBefore = System.nanoTime();
        doAnalysis();
        this.runtime = (int) Math.round((System.nanoTime() - timeBefore) / 1E3);
    }

    private Local getLocal(Value value) {
        if (value instanceof InvokeExpr)
            return null;

        Value base = BaseSelector.selectBase(value,false);
        if (base instanceof InstanceFieldRef)
            return (Local) ((InstanceFieldRef) base).getBase();
        else if (base instanceof StaticFieldRef)
            return null;
        else if (base instanceof Local)
            return (Local) base;
        return null;
    }

    private boolean isPrimitiveOrString(Type t) {
        return t instanceof PrimType || TypeUtils.isStringType(t);
    }

    @Override
    protected void flowThrough(FlowSet<Local> inSet, Unit unit, FlowSet<Local> outSet) {
        if ((this.timeBefore / 1E9) > 60) {
            outSet.clear();
            return;
        }

        assert !inSet.contains(null);
        if (!(unit instanceof Stmt))
            return;

        // Taint sink
        if (inSet.isEmpty()) {
            if (unit != sink)
                return;
            if (!sink.containsInvokeExpr())
                return;
            InvokeExpr ie = sink.getInvokeExpr();
            if (ie instanceof InstanceInvokeExpr) {
                Local baseObject = getLocal(((InstanceInvokeExpr) ie).getBase());
                if (baseObject != null)
                    outSet.add(baseObject);
            }
            for (Value arg : ie.getArgs()) {
                if (arg instanceof Local)
                    outSet.add((Local) arg);
            }
            return;
        }

        // Count only before the sink
        propagations++;

        // Taints stay alive by default
        inSet.copy(outSet);

        // Assign Dataflow
        if (unit instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt) unit;
            Local left = getLocal(assignStmt.getLeftOp());
            Local right = getLocal(assignStmt.getRightOp());
            if (inSet.contains(left)) {
                outSet.remove(left);
                if (right != null)
                    outSet.add(right);
            }
        }

        // Calls
        if (((Stmt) unit).containsInvokeExpr()) {
            Stmt stmt = (Stmt) unit;
            InvokeExpr ie = stmt.getInvokeExpr();
            // Arguments
            for (Value arg : ie.getArgs()) {
                if (arg instanceof Local && !isPrimitiveOrString(arg.getType()) && inSet.contains((Local) arg))
                    flowsIntoCallee++;
            }
            // Base Object
            Local baseObject = null;
            if (ie instanceof InstanceInvokeExpr) {
                baseObject = getLocal(((InstanceInvokeExpr) ie).getBase());
                if (inSet.contains(baseObject))
                    flowsIntoCallee++;
            }
            if (stmt instanceof AssignStmt) {
                Local left = getLocal(((AssignStmt) stmt).getLeftOp());
                if (inSet.contains(left)) {
                    flowsIntoCallee++;
                    for (Value arg : ie.getArgs()) {
                        if (arg instanceof Local)
                            outSet.add((Local) arg);
                    }
                    if (baseObject != null)
                        outSet.add(baseObject);
                }
            }
        }

        // Reached the end of the method
        if (unit instanceof IdentityStmt) {
            // Parameters
            for (Local param : method.getActiveBody().getParameterLocals()) {
                if (inSet.contains(param)) {
                    flowsIntoCaller++;
                    outSet.remove(param);
                }
            }
            // this
            Local thisLocal = method.isStatic() ? null : method.getActiveBody().getThisLocal();
            if (thisLocal != null && inSet.contains(thisLocal)) {
                flowsIntoCaller++;
                outSet.remove(thisLocal);
            }
        }
//        System.out.println(unit.toString() + "\n" + inSet.toString() + "\n" + outSet.toString());
    }

    @Override
    protected FlowSet<Local> newInitialFlow() {
        return new ArraySparseSet<>();
    }

    @Override
    protected void merge(FlowSet<Local> in1, FlowSet<Local> in2, FlowSet<Local> out) {
        in1.union(in2, out);
    }

    @Override
    protected void copy(FlowSet<Local> in, FlowSet<Local> out) {
        in.copy(out);
    }

    public int getFlowsIntoCallee() {
        return flowsIntoCallee;
    }
    public int getFlowsIntoCaller() {
        return flowsIntoCaller;
    }
    public int getPropagations() {
        return propagations;
    }
    public int getTaintNumInSets() { return unitToBeforeFlow.values().stream().mapToInt(FlowSet::size).sum(); }
    public int getTaintNumOutSets() { return unitToAfterFlow.values().stream().mapToInt(FlowSet::size).sum(); }
    public int getRuntime() {
        return runtime;
    }
}
