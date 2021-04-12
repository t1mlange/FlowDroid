package soot.jimple.infoflow.util.preanalyses;

import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.util.BaseSelector;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

/**
 * Intraprocedural forward taint analysis without access paths using EasyTaintWrapper alike rules.
 * Intentionally imprecise and unsound. Idea: Find out for which direction the precise analysis performs better.
 *
 * @author Tim Lange
 */
public class SourceAnalysis extends ForwardFlowAnalysis<Unit, FlowSet<Local>> {
    SootMethod method;
    Stmt source;
    int flowsIntoCallee = 0;
    int flowsIntoCaller = 0;
    int propagations = 0;
    int runtime ;

    public SourceAnalysis(DirectedGraph<Unit> graph, SootMethod method, Stmt source) {
        super(graph);
        this.method = method;
        this.source = source;

        long timeBefore = System.nanoTime();
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

    @Override
    protected void flowThrough(FlowSet<Local> inSet, Unit unit, FlowSet<Local> outSet) {
        assert !inSet.contains(null);
        if (!(unit instanceof Stmt))
            return;

        // Taint source
        if (inSet.isEmpty()) {
            if (unit != source)
                return;

            if (source instanceof AssignStmt) {
                Local retVal = getLocal(((AssignStmt) source).getLeftOp());
                if (retVal != null)
                    outSet.add(retVal);
            } else if (source instanceof InstanceInvokeExpr) {
                Local baseObject = getLocal(((InstanceInvokeExpr) source).getBase());
                if (baseObject != null)
                    outSet.add(baseObject);
            }
            return;
        }

        // Count only after the sink
        propagations++;

        // Taints stay alive by default
        inSet.copy(outSet);

        // Assign Dataflow
        if (unit instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt) unit;
            Local left = getLocal(assignStmt.getLeftOp());
            Local right = getLocal(assignStmt.getRightOp());
            if (inSet.contains(right)) {
                if (left != null)
                    outSet.add(left);
            } else if (inSet.contains(left)) {
                outSet.remove(left);
            }
        }

        // Calls
        if (((Stmt) unit).containsInvokeExpr()) {
            Stmt stmt = (Stmt) unit;
            InvokeExpr ie = stmt.getInvokeExpr();
            Local baseObject = null;
            boolean tainted = false;
            // Base Object
            if (ie instanceof InstanceInvokeExpr) {
                baseObject = getLocal(((InstanceInvokeExpr) ie).getBase());
                if (inSet.contains(baseObject)) {
                    flowsIntoCallee++;
                    tainted = true;
                }
            }
            // Arguments
            for (Value arg : ie.getArgs()) {
                if (arg instanceof Local && inSet.contains((Local) arg)) {
                    flowsIntoCallee++;
                    tainted = true;
                }
            }
            if (tainted) {
                if (stmt instanceof AssignStmt) {
                    Local left = getLocal(((AssignStmt) stmt).getLeftOp());
                    if (left != null)
                        outSet.add(left);
                }
                if (baseObject != null)
                    outSet.add(baseObject);
            }
        }

        // Reached one possible end
        if (unit instanceof ReturnStmt) {
            // Return Op
            ReturnStmt returnStmt = (ReturnStmt) unit;
            Local op = getLocal(returnStmt.getOp());
            if (inSet.contains(op)) {
                flowsIntoCaller++;
                outSet.remove(op);
            }
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
