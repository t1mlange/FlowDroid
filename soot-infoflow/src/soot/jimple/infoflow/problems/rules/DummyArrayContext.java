package soot.jimple.infoflow.problems.rules;

import soot.jimple.ArrayRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.ContextDefinition;

public class DummyArrayContext implements IArrayContextProvider {
    public ContextDefinition[] getContextForArrayRef(ArrayRef arrayRef, Stmt stmt) {
        return null;
    }
}
