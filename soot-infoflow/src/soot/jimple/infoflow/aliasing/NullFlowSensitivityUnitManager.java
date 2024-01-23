package soot.jimple.infoflow.aliasing;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;

public class NullFlowSensitivityUnitManager implements IFlowSensitivityUnitManager {
    @Override
    public Abstraction registerCallSite(Unit callSite, SootMethod callee, Abstraction activationAbs, Abstraction prev) {
        return activationAbs;
    }

    @Override
    public boolean isCallSiteActivatingTaint(Unit callSite, Unit activationUnit) {
        return false;
    }
}
