package soot.jimple.infoflow.collections.strategies.containers;

import soot.Value;
import soot.jimple.Constant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.CollectionTaintWrapper;
import soot.jimple.infoflow.collections.context.KeySetContext;
import soot.jimple.infoflow.collections.context.UnknownContext;
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
            return ((KeySetContext<?>) apKey).intersect((KeySetContext<?>) stmtKey);

        throw new RuntimeException("Got unknown context: " + apKey.getClass());
    }

    @Override
    public ContextDefinition[] append(ContextDefinition[] ctxt1, ContextDefinition[] ctxt2) {
        // putAll in maps can be easily modelled as a copy operation
        // append is only needed for lists and derivatives
        return null;
    }

    @Override
    public ContextDefinition getKeyContext(Value value, Stmt stmt) {
        if (value instanceof Constant) {
            resolvedKeys++;
            return new KeySetContext<>((Constant) value);
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
    public ContextDefinition shift(ContextDefinition ctxt, ContextDefinition n, boolean exact) {
        return UnknownContext.v();
    }

    @Override
    public ContextDefinition rotate(ContextDefinition ctxt, Stmt stmt, ContextDefinition n, ContextDefinition bound, boolean exact) {
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
}
