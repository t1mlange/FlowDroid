package soot.jimple.infoflow.collections.operations.forward;

import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.data.ContextDefinition;

/**
 * Operation that invalidates all indices if they might be affected by a shift.
 * <br>
 * Shifting is expensive (complex operation plus widening requires traversing the abstraction graph), thus we assume
 * that shifting will significantly impact the runtime. A right shift is quite rare, while a left shift is more common
 * but not super common (5000 of 467,223 operations on lists). Using this operation instead of {@link ShiftLeftOperation}
 * and {@link ShiftRightOperation} allows us to run the IFDS solver without widening while keeping the index as long as
 * it is not shifted.
 *
 * @author Tim Lange
 */
public class ShiftInvalidateOperation extends AbstractShiftOperation {
    public ShiftInvalidateOperation(Location[] keys, String field) {
        super(keys, field);
    }

    @Override
    protected ContextDefinition shift(ContextDefinition ctxt, Stmt stmt, boolean exact, IContainerStrategy strategy) {
        return UnknownContext.v();
    }
}
