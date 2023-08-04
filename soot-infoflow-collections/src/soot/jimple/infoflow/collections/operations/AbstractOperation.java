package soot.jimple.infoflow.collections.operations;

import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.data.ParamIndex;
import soot.jimple.infoflow.typing.TypeUtils;

public abstract class AbstractOperation implements ICollectionOperation {
    protected SootField safeGetField(String fieldSig) {
        if (fieldSig == null || fieldSig.isEmpty())
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

    protected Value getValueFromIndex(int idx, Stmt stmt) {
        Value value;
        switch (idx) {
            case ParamIndex.BASE:
                value = stmt.getInvokeExpr() instanceof InstanceInvokeExpr
                            ? ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase() : null;
                break;
            case ParamIndex.RETURN:
                value = stmt instanceof AssignStmt ? ((AssignStmt) stmt).getLeftOp() : null;
                break;
            default:
                if (idx < 0)
                    throw new RuntimeException("Unexpected to index: " + idx);
                value = stmt.getInvokeExpr().getArg(idx);
        }
        return value;
    }
}
