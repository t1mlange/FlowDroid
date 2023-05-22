package soot.jimple.infoflow.collections.operations;

import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.data.ContextDefinition;

public class ShiftLeftOperation extends AbstractShiftOperation {

    public ShiftLeftOperation(Location[] keys, String field, String fieldType) {
        super(keys, field, fieldType);
    }

    @Override
    protected ContextDefinition shift(ContextDefinition ctxt, Stmt stmt, boolean exact, IContainerStrategy strategy) {
        return strategy.shiftLeft(ctxt, stmt, exact);
    }
}
