package soot.jimple.infoflow.collections.strategies.containers.shift;

import soot.jimple.infoflow.data.ContextDefinition;

public interface IShiftOperation {
    ContextDefinition shift(ContextDefinition ctxt, int n, boolean exact);
}
