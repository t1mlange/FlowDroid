package soot.jimple.infoflow.collections.strategies.containers.shift;

import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.data.ContextDefinition;

public class InvalidateShift implements IShiftOperation {
    @Override
    public ContextDefinition shift(ContextDefinition ctxt, int n, boolean exact) {
        return UnknownContext.v();
    }
}
