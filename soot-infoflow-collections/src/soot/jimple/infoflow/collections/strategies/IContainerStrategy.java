package soot.jimple.infoflow.collections.strategies;

import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

public interface IContainerStrategy {
    /**
     *
     * @param apKey   key from the access path
     * @param stmtKey key from the statement
     * @return
     */
    Tristate intersect(ContextDefinition apKey, ContextDefinition stmtKey);

    /**
     * Retrieves a context for a given key
     *
     * @param value
     * @param stmt
     * @return
     */
    ContextDefinition getContextFromKey(Value value, Stmt stmt);

    /**
     * Retrieves a context given an implicit key
     * (i.e. a list where the key is dependent on the number of add calls before)
     *
     * @param value
     * @param stmt
     * @return
     */
    ContextDefinition getContextFromImplicitKey(Value value, Stmt stmt);

    /**
     * Returns whether the context is still useful or not
     *
     * @param ctxts contexts
     * @return true if context contains no useful information and thus, the collection should be smashed
     */
    boolean shouldSmash(ContextDefinition[] ctxts);
}
