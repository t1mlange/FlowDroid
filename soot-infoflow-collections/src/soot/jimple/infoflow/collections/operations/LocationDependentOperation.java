package soot.jimple.infoflow.collections.operations;

import soot.jimple.infoflow.collections.data.Location;

public abstract class LocationDependentOperation implements ICollectionOperation {
    protected final Location[] keys;
    protected final String field;
    protected final String fieldType;

    public LocationDependentOperation(Location[] keys, String field, String fieldType) {
        this.keys = keys;
        this.field = field;
        this.fieldType = fieldType;
    }
}
