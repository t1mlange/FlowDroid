package soot.jimple.infoflow.collections.operations;

import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.data.Abstraction;

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
