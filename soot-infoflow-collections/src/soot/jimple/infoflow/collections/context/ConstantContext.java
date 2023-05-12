package soot.jimple.infoflow.collections.context;

import soot.jimple.Constant;
import soot.jimple.infoflow.data.ContextDefinition;

public class ConstantContext implements ContextDefinition {
    private final Constant c;

    public ConstantContext(Constant c) {
        this.c = c;
    }

    public Constant getConstant() {
        return c;
    }

    @Override
    public String toString() {
        return c.toString();
    }
}
