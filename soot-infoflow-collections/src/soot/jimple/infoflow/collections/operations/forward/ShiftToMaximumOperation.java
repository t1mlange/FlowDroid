package soot.jimple.infoflow.collections.operations.forward;

import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.data.ContextDefinition;

/**
 * Operation that invalidates all indices if they might be affected by a shift.
 * <br>
 * Shifting is expensive (complex operation plus widening requires traversing the abstraction graph), thus we assume
 * that shifting will significantly impact the runtime. Using this operation instead of {@link ShiftRightOperation}
 * allows us to run the IFDS solver without widening while keeping the minimum index in mind.
 *
 * @author Tim Lange
 */
public class ShiftToMaximumOperation extends AbstractShiftOperation {
    public ShiftToMaximumOperation(Location[] keys, String field) {
        super(keys, field);
    }

    @Override
    protected ContextDefinition shift(ContextDefinition ctxt, Stmt stmt, boolean exact, IContainerStrategy strategy) {
        if (ctxt instanceof IntervalContext)
            return new IntervalContext(((IntervalContext) ctxt).getMin(), Integer.MAX_VALUE);
        return UnknownContext.v();
    }
}
