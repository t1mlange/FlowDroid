package soot.jimple.infoflow.collections.strategies.containers;

import java.util.concurrent.ConcurrentHashMap;

import soot.Local;
import soot.SootMethod;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.CollectionTaintWrapper;
import soot.jimple.infoflow.collections.analyses.ListSizeAnalysis;
import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.collections.context.KeySetContext;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

/**
 * Strategy that reasons about maps with constant keys and lists with constant indices.
 * Uses {@link ListSizeAnalysis} to retrieve the list size, thus, should only be used for testing
 * the semantics of list operations.
 *
 * @author Tim Lange
 */
public class TestConstantStrategy extends ConstantMapStrategy {
    // Benign race on the counters because they are on the critical path within the data flow analysis
    private long resolvedIndices;
    private long unresolvedIndices;

    private final ConcurrentHashMap<SootMethod, ListSizeAnalysis> implicitIndices;
    protected final InfoflowManager manager;

    public TestConstantStrategy(InfoflowManager manager, CollectionTaintWrapper ctw) {
        super();
        this.implicitIndices = new ConcurrentHashMap<>();
        this.manager = manager;
    }

    public long getResolvedIndices() {
        return resolvedIndices;
    }

    public long getUnresolvedIndices() {
        return unresolvedIndices;
    }

    @Override
    public Tristate intersect(ContextDefinition apKey, ContextDefinition stmtKey) {
        if (apKey == UnknownContext.v() || stmtKey == UnknownContext.v())
            return Tristate.MAYBE();

        if (apKey instanceof IntervalContext)
            return ((IntervalContext) apKey).intersects((IntervalContext) stmtKey);
        if (apKey instanceof KeySetContext)
            return ((KeySetContext<?>) apKey).intersect((KeySetContext<?>) stmtKey);

        throw new RuntimeException("Got unknown context: " + apKey.getClass());
    }

    @Override
    public ContextDefinition getIndexContext(Value value, Stmt stmt) {
        if (value instanceof IntConstant) {
            resolvedIndices++;
            return new IntervalContext(((IntConstant) value).value);
        }

        unresolvedIndices++;
        return UnknownContext.v();
    }

    @Override
    public ContextDefinition getNextPosition(Value value, Stmt stmt) {
        return getContextFromImplicitKey(value, stmt, false);
    }

    @Override
    public ContextDefinition getFirstPosition(Value value, Stmt stmt) {
        resolvedIndices++;
        return new IntervalContext(0);
    }

    @Override
    public ContextDefinition getLastPosition(Value value, Stmt stmt) {
        return getContextFromImplicitKey(value, stmt, true);
    }

    @Override
    public Tristate lessThanEqual(ContextDefinition ctxt1, ContextDefinition ctxt2) {
        if (ctxt1 == UnknownContext.v() || ctxt2 == UnknownContext.v())
            return Tristate.MAYBE();

        if (ctxt1 instanceof IntervalContext && ctxt2 instanceof IntervalContext)
            return ((IntervalContext) ctxt1).lessThanEqual(((IntervalContext) ctxt2));

        throw new RuntimeException("Unknown combination of " + ctxt1.toString() + " and " + ctxt2.toString());
    }

    @Override
    public ContextDefinition shift(ContextDefinition ctxt, ContextDefinition n, boolean exact) {
        if (ctxt instanceof IntervalContext && n instanceof IntervalContext)
            return exact
                    ? ((IntervalContext) ctxt).exactShift((IntervalContext) n)
                    : ((IntervalContext) ctxt).mayShift((IntervalContext) n);

        throw new RuntimeException("Expect two interval contexts but got: " + ctxt.getClass().getName() + " and " + n.getClass().getName());
    }

    @Override
    public ContextDefinition rotate(ContextDefinition ctxt, Stmt stmt, ContextDefinition n, ContextDefinition bound, boolean exact) {
        if (ctxt instanceof IntervalContext && n instanceof IntervalContext && bound instanceof IntervalContext) {
            IntervalContext dist = (IntervalContext) n;
            IntervalContext mod = (IntervalContext) bound;
            return exact ? ((IntervalContext) ctxt).exactRotate(dist, mod) : ((IntervalContext) ctxt).mayRotate(dist, mod);
        }
        throw new RuntimeException("Expect interval context but got instead: " + ctxt);
    }


    @Override
    public ContextDefinition[] append(ContextDefinition[] ctxt1, ContextDefinition[] ctxt2) {
        // Shifting only occurs on lists
        if (ctxt1.length != ctxt2.length || ctxt1.length != 1 || !(ctxt1[0] instanceof IntervalContext))
            return null;

        ContextDefinition[] newCtxt = new ContextDefinition[1];
        newCtxt[0] = shift(ctxt2[0], ctxt1[0], true);
        return newCtxt;
    }

    private ContextDefinition getContextFromImplicitKey(Value value, Stmt stmt, boolean decr) {
        if (value instanceof Local) {
            SootMethod currMethod = manager.getICFG().getMethodOf(stmt);
            var lstSizeAnalysis = implicitIndices.computeIfAbsent(currMethod,
                    sm -> new ListSizeAnalysis(manager.getICFG().getOrCreateUnitGraph(sm)));
            ListSizeAnalysis.ListSize size = lstSizeAnalysis.getFlowBefore(stmt).get(value);
            if (size != null && !size.isBottom()) {
                resolvedIndices++;
                return new IntervalContext(decr ? size.getSize() - 1 : size.getSize());
            }
        }

        unresolvedIndices++;
        return UnknownContext.v();
    }
}
