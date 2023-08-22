package soot.jimple.infoflow.collections.strategies.containers;

import java.util.concurrent.ConcurrentHashMap;

import soot.Local;
import soot.SootMethod;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
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
public class TestConstantStrategyShiftWidening extends TestConstantStrategy {
    public TestConstantStrategyShiftWidening(InfoflowManager manager) {
        super(manager);
    }

    @Override
    public ContextDefinition shift(ContextDefinition ctxt, ContextDefinition n, boolean exact) {
        if (ctxt instanceof IntervalContext && n instanceof IntervalContext) {
            IntervalContext in = (IntervalContext) n;
            int min = in.getMin() < 0 ? 0 : in.getMin();
            int max = in.getMax() > 0 ? Integer.MAX_VALUE : in.getMax();
            return new IntervalContext(min, max);
        }

        throw new RuntimeException("Expect two interval contexts but got: " + ctxt.getClass().getName() + " and " + n.getClass().getName());
    }

}
