package soot.jimple.infoflow.collections.operations.alias;

import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.operations.forward.ShiftRightOperation;

public class AliasShiftLeftOperation extends ShiftRightOperation {
    public AliasShiftLeftOperation(Location[] keys, String field) {
        super(keys, field);
    }
}
