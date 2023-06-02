package soot.jimple.infoflow.collections.operations.forward.alias;

import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.operations.forward.RemoveOperation;

public class AliasRemoveOperation extends RemoveOperation {
    public AliasRemoveOperation(Location[] keys, String field) {
        super(keys, field);
    }
}
