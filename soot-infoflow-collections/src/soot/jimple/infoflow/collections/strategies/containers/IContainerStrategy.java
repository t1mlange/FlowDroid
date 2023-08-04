package soot.jimple.infoflow.collections.strategies.containers;

import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

/**
 * Strategy for resolving keys/indices and reasoning about their relation
 *
 * @author Tim Lange
 */
public interface IContainerStrategy {
    /**
     * Checks whether the two arguments intersect
     *
     * @param apKey   key from the access path
     * @param stmtKey key from the statement
     * @return true if args fully intersects, maybe on part match and false on definitely no match
     */
    Tristate intersect(ContextDefinition apKey, ContextDefinition stmtKey);

    /**
     * Retrieves a context for a given key
     *
     * @param key  requested value
     * @param stmt statement containing key
     * @return new context definition
     */
    ContextDefinition getKeyContext(Value key, Stmt stmt);

    /**
     * Retrieves a context for a given index
     *
     * @param index requested value
     * @param stmt  statement containing index
     * @return new context definition
     */
    ContextDefinition getIndexContext(Value index, Stmt stmt);

    /**
     * Retrieves a context given an implicit key after the given statement
     * (i.e. a list where the key is dependent on the number of add calls before)
     *
     * @param lst  list
     * @param stmt statement that uses value
     * @return new context definition
     */
    ContextDefinition getNextPosition(Value lst, Stmt stmt);

    /**
     * Retrieves a context given an implicit key before the given statement
     * (i.e. a list where the key is dependent on the number of add calls before)
     *
     * @param lst  list
     * @param stmt statement that uses value
     * @return new context definition
     */
    ContextDefinition getFirstPosition(Value lst, Stmt stmt);

    /**
     * Retrieves a context given an implicit key before the given statement
     * (i.e. a list where the key is dependent on the number of add calls before)
     *
     * @param lst  list
     * @param stmt statement that uses value
     * @return new context definition
     */
    ContextDefinition getLastPosition(Value lst, Stmt stmt);

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
     * @return new context definition
     */
    ContextDefinition shiftRight(ContextDefinition ctxt, Stmt stmt, boolean exact);

    /**
     * Shifts the ctxt to the right
     *
     * @param ctxt current context
     * @return new context definition
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
