//package soot.jimple.infoflow.problems;
//
//import soot.Unit;
//import soot.jimple.infoflow.data.Abstraction;
//import soot.toolkits.graph.DirectedGraph;
//import soot.toolkits.graph.UnitGraph;
//import soot.toolkits.scalar.ArraySparseSet;
//import soot.toolkits.scalar.FlowSet;
//import soot.toolkits.scalar.ForwardFlowAnalysis;
//
//import java.util.Set;
//
//public class ForwardPreAnalysis extends ForwardFlowAnalysis<Unit, FlowSet<Abstraction>> {
//    public ForwardPreAnalysis(DirectedGraph<Unit> graph) {
//        UnitGraph
//        super(graph);
//    }
//
//    public void analyze() {
//        FlowSet<Abstraction> flowSet = newInitialFlow();
//        for (Unit unit : graph){
//
//        }
//    }
//
//    @Override
//    protected void flowThrough(FlowSet<Abstraction> abstractions, Unit unit, FlowSet<Abstraction> a1) {
//
//    }
//
//    @Override
//    protected FlowSet<Abstraction> newInitialFlow() {
//        return new ArraySparseSet<>();
//    }
//
//    @Override
//    protected void merge(FlowSet<Abstraction> in1, FlowSet<Abstraction> in2, FlowSet<Abstraction> out) {
//        in1.union(in2, out);
//    }
//
//    @Override
//    protected void copy(FlowSet<Abstraction> in, FlowSet<Abstraction> out) {
//        in.copy(out);
//    }
//}
