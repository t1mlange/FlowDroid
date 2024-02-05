package soot.jimple.infoflow.aliasing.unitManager;

import soot.Unit;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;

/**
 * Checks the activation unit transitively in the call tree. Note that the actual possible call tree is
 * overapproximated and, thus, using this flow sensitivity might result in false positives.
 *
 * This strategy is the default for FlowDroid and the one used in most papers based on FlowDroid.
 */
public class DefaultActivationUnitManager extends TransitiveUnitManager {
    public DefaultActivationUnitManager(InfoflowManager manager) {
        super(manager);
        if (this.manager.getConfig().getDataFlowDirection() != InfoflowConfiguration.DataFlowDirection.Forwards)
            throw new RuntimeException("Invalid configuration! This class requires a forward analysis");
    }

    @Override
    protected Unit getFlowUnit(Abstraction abs) {
        return abs.getActivationUnit();
    }
}
