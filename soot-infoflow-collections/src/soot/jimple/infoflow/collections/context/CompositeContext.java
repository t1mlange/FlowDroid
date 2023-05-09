package soot.jimple.infoflow.collections.context;

import soot.jimple.infoflow.data.ContextDefinition;

import java.util.ArrayList;
import java.util.List;

public class CompositeContext implements ContextDefinition {
    private final List<ContextDefinition> contexts;
    private boolean unknown;

    public CompositeContext(List<ContextDefinition> contexts) {
        this.contexts = contexts;
        for (ContextDefinition ctx : contexts)
            unknown |= ctx.isUnknown();
    }

    @Override
    public boolean isUnknown() {
        return unknown;
    }

    @Override
    public boolean isComposite() {
        return true;
    }
}
