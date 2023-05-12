package soot.jimple.infoflow.collections.context;

import soot.jimple.infoflow.data.ContextDefinition;

public class UnknownContext implements ContextDefinition {
    private static final UnknownContext INSTANCE = new UnknownContext();

    public static UnknownContext v() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "<Unknown Context>";
    }
}
