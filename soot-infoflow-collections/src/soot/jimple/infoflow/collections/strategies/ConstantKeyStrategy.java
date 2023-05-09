package soot.jimple.infoflow.collections.strategies;

import soot.Value;
import soot.jimple.Constant;
import soot.jimple.infoflow.collections.context.ConstantContext;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.strategies.IKeyStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

public class ConstantKeyStrategy implements IKeyStrategy {
    @Override
    public Tristate intersect(ContextDefinition apKey, ContextDefinition stmtKey) {
        if (stmtKey.isUnknown())
            return Tristate.MAYBE();

        if (((ConstantContext) apKey).getConstant().equals(((ConstantContext) stmtKey).getConstant()))
            return Tristate.TRUE();
        return Tristate.FALSE();
    }

    @Override
    public ContextDefinition getFromValue(Value value) {
        if (value instanceof Constant)
            return new ConstantContext((Constant) value);
        return UnknownContext.v();
    }
}
