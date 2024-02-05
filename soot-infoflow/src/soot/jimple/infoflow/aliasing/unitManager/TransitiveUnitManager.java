package soot.jimple.infoflow.aliasing.unitManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.IFlowSensitivityUnitManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

public abstract class TransitiveUnitManager implements IFlowSensitivityUnitManager {
    protected final Map<Unit, CallSite> unitsToCallSites = new ConcurrentHashMap<>();

    protected final InfoflowManager manager;

    public TransitiveUnitManager(InfoflowManager manager) {
        this.manager = manager;
        if (!this.manager.getConfig().getFlowSensitiveAliasing())
            throw new RuntimeException("Invalid configuration! This class requires flow sensitive alias analysis");
    }

    protected abstract Unit getFlowUnit(Abstraction abs);

    @Override
    public boolean pathFromCallToUnitExists(Unit callSite, Unit flowUnit) {
        if (flowUnit == null)
            return false;
        CallSite callSites = unitsToCallSites.get(flowUnit);
        return callSites != null && callSites.containsCallSite(callSite);
    }

    @Override
    public Abstraction registerCallSite(Unit callSite, SootMethod callee, Abstraction activationAbs, Abstraction prev) {
        Unit unit = getFlowUnit(activationAbs);
        if (unit == null)
            return activationAbs;

        CallSite callSites = unitsToCallSites.computeIfAbsent(unit, (u) -> new CallSite());
        if (callSites.containsCallSite(callSite))
            return activationAbs;

        IInfoflowCFG icfg = manager.getICFG();
        if (!callee.getActiveBody().getUnits().contains(unit)
            && !callSites.containsCallSiteMethod(callee))
                return activationAbs;

        callSites.addCallsite(callSite, icfg);
        return activationAbs;
    }
}
