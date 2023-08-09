//package soot.jimple.infoflow.collections.operations.forward;
//
//import soot.jimple.Stmt;
//import soot.jimple.infoflow.collections.context.UnknownContext;
//import soot.jimple.infoflow.collections.data.Location;
//import soot.jimple.infoflow.collections.data.ParamIndex;
//import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
//import soot.jimple.infoflow.data.ContextDefinition;
//
//public class RotateOperation extends AbstractShiftOperation {
//    private final int data;
//
//    public RotateOperation(Location[] keys, int data, String field) {
//        super(keys, field);
//        this.data = data;
//    }
//
//    @Override
//    protected ContextDefinition shift(ContextDefinition ctxt, Stmt stmt, boolean exact, IContainerStrategy strategy) {
//        ContextDefinition lstSize = strategy.getNextPosition(getValueFromIndex(ParamIndex.BASE, stmt), stmt);
//        if (!lstSize.containsInformation())
//            return UnknownContext.v();
//
//        ContextDefinition n = strategy.getKeyContext(getValueFromIndex(data, stmt), stmt);
//        if (!n.containsInformation())
//            return UnknownContext.v();
//
//        return strategy.rotate(ctxt, stmt, n, lstSize, exact);
//    }
//}
