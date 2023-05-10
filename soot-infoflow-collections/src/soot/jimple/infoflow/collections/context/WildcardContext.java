package soot.jimple.infoflow.collections.context;

import soot.jimple.infoflow.data.ContextDefinition;

public class WildcardContext implements ContextDefinition {
    private static final WildcardContext INSTANCE = new WildcardContext();

    public static WildcardContext v() {
        return INSTANCE;
    }
}
