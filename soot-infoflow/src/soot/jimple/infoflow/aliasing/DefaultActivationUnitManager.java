package soot.jimple.infoflow.aliasing;

import polyglot.ast.For;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultActivationUnitManager implements IFlowSensitivityUnitManager {
    private final Map<Unit, CallSites> activationUnitsToCallSites = new ConcurrentHashMap<>();

    private final InfoflowManager manager;

    public DefaultActivationUnitManager(InfoflowManager manager) {
        this.manager = manager;
        if (this.manager.getConfig().getDataFlowDirection() != InfoflowConfiguration.DataFlowDirection.Forwards
            || !this.manager.getConfig().getFlowSensitiveAliasing())
            throw new RuntimeException("Invalid configuration! This class requires forward flow sensitive alias analysis");
    }

    @Override
    public boolean isCallSiteActivatingTaint(Unit callSite, Unit activationUnit) {
        if (!manager.getConfig().getFlowSensitiveAliasing())
            return false;

        if (activationUnit == null)
            return false;
        CallSites callSites = activationUnitsToCallSites.get(activationUnit);
        if (callSites != null)
            return callSites.containsCallSite(callSite);
        return false;
    }

    @Override
    public Abstraction registerCallSite(Unit callSite, SootMethod callee, Abstraction activationAbs, Abstraction prev) {
        if (!manager.getConfig().getFlowSensitiveAliasing())
            return activationAbs;
        Unit activationUnit = activationAbs.getActivationUnit();
        if (activationUnit == null)
            return activationAbs;

        CallSites callSites = activationUnitsToCallSites.computeIfAbsent(activationUnit, (u) -> new CallSites());
        if (callSites.containsCallSite(callSite))
            return activationAbs;

        IInfoflowCFG icfg = manager.getICFG();
        if (!activationAbs.isAbstractionActive()) {
            if (!callee.getActiveBody().getUnits().contains(activationUnit)) {
                if (!callSites.containsCallSiteMethod(callee))
                    return activationAbs;
            }
        }

        callSites.addCallsite(callSite, icfg);
        return activationAbs;
    }
}
