package soot.jimple.infoflow.aliasing.unitManager;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.aliasing.IFlowSensitivityUnitManager;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MergeReplayActivationUnitManager implements IFlowSensitivityUnitManager {
    private final Map<Unit, CallSite> activationUnitsToCallSites = new ConcurrentHashMap<>();

    @Override
    public Abstraction registerCallSite(Unit callSite, SootMethod callee, Abstraction activationAbs, Abstraction prev) {
        assert !activationAbs.isAbstractionActive(); // We only register call sites backward????

        Unit activationUnit = activationAbs.getActivationUnit();
        // The global activation unit abstracts the flows in the backward analysis
        if (activationUnit == SymbolicActivationUnit.GAU)
            return activationAbs;

        CallSite callSites = activationUnitsToCallSites.computeIfAbsent(activationUnit, (u) -> new CallSite());
        if (callSites.containsCallSite(callSite))
            return activationAbs;

        if (activationUnit instanceof SymbolicActivationUnit) {

        } else {

        }

        return null;
    }

    @Override
    public boolean isCallSiteActivatingTaint(Unit callSite, Unit activationUnit) {
        // Can happen in both directions

        return false;
    }


}
