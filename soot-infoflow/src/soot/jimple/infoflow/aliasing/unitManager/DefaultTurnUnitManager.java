package soot.jimple.infoflow.aliasing.unitManager;

import soot.Unit;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;

/**
 * Checks the turn unit transitively in the call tree. Note that the actual possible call tree is overapproximated and
 * the turn unit kills flows, thus, using this flow sensitivity might result in false negatives.
 */
public class DefaultTurnUnitManager extends TransitiveUnitManager {
    public DefaultTurnUnitManager(InfoflowManager manager) {
        super(manager);
        if (this.manager.getConfig().getDataFlowDirection() != InfoflowConfiguration.DataFlowDirection.Backwards)
            throw new RuntimeException("Invalid configuration! This class requires a backward analysis");
    }

    @Override
    protected Unit getFlowUnit(Abstraction abs) {
        return abs.getTurnUnit();
    }
}
