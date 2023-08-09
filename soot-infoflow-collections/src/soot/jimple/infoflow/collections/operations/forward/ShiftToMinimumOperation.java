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
 * that shifting will significantly impact the runtime. Using this operation instead of {@link ShiftLeftOperation}
 * allows us to run the IFDS solver without widening while keeping the maximum index in mind.
 *
 * @author Tim Lange
 */
public class ShiftToMinimumOperation extends AbstractShiftOperation {
    public ShiftToMinimumOperation(Location[] keys, String field) {
        super(keys, field);
    }

    @Override
    protected ContextDefinition shift(ContextDefinition ctxt, Stmt stmt, boolean exact, IContainerStrategy strategy) {
        if (ctxt instanceof IntervalContext)
            return new IntervalContext(0, ((IntervalContext) ctxt).getMax());
        return UnknownContext.v();
    }
}
