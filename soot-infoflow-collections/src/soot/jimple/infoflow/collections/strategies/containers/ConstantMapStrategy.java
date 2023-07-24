package soot.jimple.infoflow.collections.strategies.containers;

import java.util.Arrays;

import soot.Value;
import soot.jimple.Constant;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.CollectionTaintWrapper;
import soot.jimple.infoflow.collections.context.KeySetContext;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.data.CollectionMethod;
import soot.jimple.infoflow.collections.operations.LocationDependentOperation;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

/**
 * Strategy that only reasons about maps with constant keys
 *
 * @author Tim Lange
 */
public class ConstantMapStrategy implements IContainerStrategy {
    // Benign race on the counters because they are on the critical path within the data flow analysis
    private long resolvedKeys;
    private long unresolvedKeys;

    private final CollectionTaintWrapper ctw;

    public ConstantMapStrategy(CollectionTaintWrapper ctw) {
        this.ctw = ctw;
    }

    public long getResolvedKeys() {
        return resolvedKeys;
    }

    public long getUnresolvedKeys() {
        return unresolvedKeys;
    }

    @Override
    public Tristate intersect(ContextDefinition apKey, ContextDefinition stmtKey) {
        if (apKey == UnknownContext.v() || stmtKey == UnknownContext.v())
            return Tristate.MAYBE();

        if (apKey instanceof KeySetContext)
            return ((KeySetContext) apKey).intersect((KeySetContext) stmtKey);

        throw new RuntimeException("Got unknown context: " + apKey.getClass());
    }

    @Override
    public ContextDefinition getKeyContext(Value value, Stmt stmt) {
        if (value instanceof Constant) {
            resolvedKeys++;
            return new KeySetContext((Constant) value);
        }

        unresolvedKeys++;
        return UnknownContext.v();
    }

    @Override
    public ContextDefinition getIndexContext(Value value, Stmt stmt) {
        return UnknownContext.v();
    }

    @Override
    public ContextDefinition getNextPosition(Value value, Stmt stmt) {
        return UnknownContext.v();
    }

    @Override
    public ContextDefinition getFirstPosition(Value value, Stmt stmt) {
        return UnknownContext.v();
    }

    @Override
    public ContextDefinition getLastPosition(Value value, Stmt stmt) {
        return UnknownContext.v();
    }

    @Override
    public Tristate lessThanEqual(ContextDefinition ctxt1, ContextDefinition ctxt2) {
        return Tristate.MAYBE();
    }

    @Override
    public ContextDefinition shiftRight(ContextDefinition ctxt, Stmt stmt, boolean exact) {
        return UnknownContext.v();
    }

    @Override
    public ContextDefinition shiftLeft(ContextDefinition ctxt, Stmt stmt, boolean exact) {
        return UnknownContext.v();
    }

    @Override
    public boolean shouldSmash(ContextDefinition[] ctxts) {
        for (ContextDefinition ctxt : ctxts) {
            if (ctxt.containsInformation())
                return false;
        }

        return true;
    }

    @Override
    public boolean stmtDependsOnContext(Stmt stmt) {
        if (!stmt.containsInvokeExpr())
            return false;
        CollectionMethod cm = ctw.getCollectionMethodForSootMethod(stmt.getInvokeExpr().getMethod());
        if (cm == null)
            return false;

        return Arrays.stream(cm.operations())
                .filter(op -> op instanceof LocationDependentOperation)
                .map(op -> ((LocationDependentOperation) op).buildContext(this,
                        (InstanceInvokeExpr) stmt.getInvokeExpr(), stmt))
                .anyMatch(ctxt -> ctxt != null && !shouldSmash(ctxt));
    }
}
