package soot.jimple.infoflow.collections.operations.forward;

import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.data.ContextDefinition;

public class ShiftRightOperation extends AbstractShiftOperation {

    public ShiftRightOperation(Location[] keys, String field) {
        super(keys, field);
    }

    @Override
    protected ContextDefinition shift(ContextDefinition ctxt, Stmt stmt, boolean exact, IContainerStrategy strategy) {
        return strategy.shift(ctxt, new IntervalContext(1), exact);
    }
}
