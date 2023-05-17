package soot.jimple.infoflow.collections.context;

import soot.jimple.Constant;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.HashSet;
import java.util.Set;

public class ConstantContext implements ContextDefinition {
    private final Set<Constant> c;

    public ConstantContext(Constant c) {
        this.c = new HashSet<>();
        this.c.add(c);
    }

    public ConstantContext(Set<Constant> c) {
        this.c = c;
    }

    public Set<Constant> getConstants() {
        return c;
    }

    @Override
    public String toString() {
        return c.toString();
    }
}
