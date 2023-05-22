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
    ContextDefinition getKeyContext(Value value, Stmt stmt);

    /**
     *
     *
     * @param value
     * @param stmt
     * @return
     */
    ContextDefinition getIndexContext(Value value, Stmt stmt);

    /**
     * Retrieves a context given an implicit key after the given statement
     * (i.e. a list where the key is dependent on the number of add calls before)
     *
     * @param value
     * @param stmt
     * @return
     */
    ContextDefinition getNextPosition(Value value, Stmt stmt);

    /**
     * Retrieves a context given an implicit key before the given statement
     * (i.e. a list where the key is dependent on the number of add calls before)
     *
     * @param value
     * @param stmt
     * @return
     */
    ContextDefinition getFirstPosition(Value value, Stmt stmt);

    /**
     * Retrieves a context given an implicit key before the given statement
     * (i.e. a list where the key is dependent on the number of add calls before)
     *
     * @param value
     * @param stmt
     * @return
     */
    ContextDefinition getLastPosition(Value value, Stmt stmt);

    /**
     * Returns whether ctxt1 is less than ctxt2
     *
     * @param ctxt1 first context
     * @param ctxt2 second context
     * @return true ctxt1 is less than ctxt2
     */
    Tristate lessThanEqual(ContextDefinition ctxt1, ContextDefinition ctxt2);

    /**
     * Shifts the ctxt to the right
     *
     * @param ctxt current context
     * @return new context
     */
    ContextDefinition shiftRight(ContextDefinition ctxt, Stmt stmt, boolean exact);

    /**
     * Shifts the ctxt to the right
     *
     * @param ctxt current context
     * @return new context
     */
    ContextDefinition shiftLeft(ContextDefinition ctxt, Stmt stmt, boolean exact);

    /**
     * Returns whether the context is still useful or not
     *
     * @param ctxts contexts
     * @return true if context contains no useful information and thus, the collection should be smashed
     */
    boolean shouldSmash(ContextDefinition[] ctxts);
}
