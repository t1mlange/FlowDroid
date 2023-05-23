package soot.jimple.infoflow.collections.operations;

import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Collection;

public class ShiftRightOperation extends AbstractShiftOperation {

    public ShiftRightOperation(Location[] keys, String field) {
        super(keys, field);
    }

    @Override
    protected ContextDefinition shift(ContextDefinition ctxt, Stmt stmt, boolean exact, IContainerStrategy strategy) {
        return strategy.shiftRight(ctxt, stmt, exact);
    }
}
