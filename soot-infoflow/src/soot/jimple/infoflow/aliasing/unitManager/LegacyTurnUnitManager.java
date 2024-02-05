package soot.jimple.infoflow.aliasing.unitManager;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.IFlowSensitivityUnitManager;
import soot.jimple.infoflow.data.Abstraction;

/**
 * Checks turn units one level deep into the call tree. Was the default behavior before.
 */
public class LegacyTurnUnitManager implements IFlowSensitivityUnitManager {
    private final InfoflowManager manager;

    public LegacyTurnUnitManager(InfoflowManager manager) {
        this.manager = manager;
        if (this.manager.getConfig().getDataFlowDirection() != InfoflowConfiguration.DataFlowDirection.Backwards
                || !this.manager.getConfig().getFlowSensitiveAliasing())
            throw new RuntimeException("Invalid configuration! This class requires flow sensitive alias analysis");
    }

    @Override
    public Abstraction registerCallSite(Unit callSite, SootMethod callee, Abstraction activationAbs, Abstraction prev) {
        return activationAbs;
    }

    @Override
    public boolean pathFromCallToUnitExists(Unit callSite, Unit flowUnit) {
        if (flowUnit == null)
            return false;

        return callSite == flowUnit
                || manager.getICFG().getCalleesOfCallAt(callSite).contains(manager.getICFG().getMethodOf(flowUnit));
    }
}
