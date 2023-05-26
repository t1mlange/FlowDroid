package soot.jimple.infoflow.collections.operations.alias;

import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.operations.forward.ShiftLeftOperation;

public class AliasShiftRightOperation extends ShiftLeftOperation {
    public AliasShiftRightOperation(Location[] keys, String field) {
        super(keys, field);
    }
}
