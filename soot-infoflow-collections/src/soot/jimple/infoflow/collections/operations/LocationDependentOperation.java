package soot.jimple.infoflow.collections.operations;

import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.typing.TypeUtils;

public abstract class LocationDependentOperation implements ICollectionOperation {
    protected final Location[] locations;
    protected final String field;
    protected final String fieldType;

    public LocationDependentOperation(Location[] locations, String field, String fieldType) {
        this.locations = locations;
        this.field = field;
        this.fieldType = fieldType;
    }

    protected SootField safeGetField(String fieldSig) {
        if (fieldSig == null || fieldSig.equals(""))
            return null;

        SootField sf = Scene.v().grabField(fieldSig);
        if (sf != null)
            return sf;

        // This field does not exist, so we need to create it
        String className = fieldSig.substring(1);
        className = className.substring(0, className.indexOf(":"));
        SootClass sc = Scene.v().getSootClassUnsafe(className, true);
        if (sc.resolvingLevel() < SootClass.SIGNATURES && !sc.isPhantom()) {
            System.err.println("WARNING: Class not loaded: " + sc);
            return null;
        }

        String type = fieldSig.substring(fieldSig.indexOf(": ") + 2);
        type = type.substring(0, type.indexOf(" "));

        String fieldName = fieldSig.substring(fieldSig.lastIndexOf(" ") + 1);
        fieldName = fieldName.substring(0, fieldName.length() - 1);

        SootFieldRef ref = Scene.v().makeFieldRef(sc, fieldName, TypeUtils.getTypeFromString(type), false);
        return ref.resolve();
    }
}
