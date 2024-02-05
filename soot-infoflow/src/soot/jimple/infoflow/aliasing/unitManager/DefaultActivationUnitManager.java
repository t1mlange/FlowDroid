package soot.jimple.infoflow.aliasing.unitManager;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.IFlowSensitivityUnitManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultActivationUnitManager implements IFlowSensitivityUnitManager {
    protected final Map<Unit, CallSite> activationUnitsToCallSites = new ConcurrentHashMap<>();

    protected final InfoflowManager manager;

    public DefaultActivationUnitManager(InfoflowManager manager) {
        this.manager = manager;
        if (this.manager.getConfig().getDataFlowDirection() != InfoflowConfiguration.DataFlowDirection.Forwards
            || !this.manager.getConfig().getFlowSensitiveAliasing())
            throw new RuntimeException("Invalid configuration! This class requires forward flow sensitive alias analysis");
    }

    @Override
    public boolean isCallSiteActivatingTaint(Unit callSite, Unit activationUnit) {
        if (activationUnit == null)
            return false;
        CallSite callSites = activationUnitsToCallSites.get(activationUnit);
        return callSites != null && callSites.containsCallSite(callSite);
    }

    @Override
    public Abstraction registerCallSite(Unit callSite, SootMethod callee, Abstraction activationAbs, Abstraction prev) {
        Unit activationUnit = activationAbs.getActivationUnit();
        if (activationUnit == null)
            return activationAbs;

        CallSite callSites = activationUnitsToCallSites.computeIfAbsent(activationUnit, (u) -> new CallSite());
        if (callSites.containsCallSite(callSite))
            return activationAbs;

        IInfoflowCFG icfg = manager.getICFG();
        if (!callee.getActiveBody().getUnits().contains(activationUnit)
            && !callSites.containsCallSiteMethod(callee))
                return activationAbs;

        callSites.addCallsite(callSite, icfg);
        return activationAbs;
    }
}
