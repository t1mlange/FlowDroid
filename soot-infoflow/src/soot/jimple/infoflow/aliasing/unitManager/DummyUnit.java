package soot.jimple.infoflow.aliasing.unitManager;

import soot.jimple.internal.JNopStmt;

public class DummyUnit extends JNopStmt {
    @Override
    public String toString() {
        return "Dummy Unit";
    }
}
