package soot.jimple.infoflow.collections.strategies;

import soot.Value;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

public interface IKeyStrategy {
    /**
     *
     * @param apKey   key from the access path
     * @param stmtKey key from the statement
     * @return
     */
    Tristate intersect(ContextDefinition apKey, ContextDefinition stmtKey);

    ContextDefinition getFromValue(Value value);
}
