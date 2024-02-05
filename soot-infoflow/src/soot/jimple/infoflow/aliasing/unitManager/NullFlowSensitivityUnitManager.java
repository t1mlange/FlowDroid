package soot.jimple.infoflow.aliasing.unitManager;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.aliasing.IFlowSensitivityUnitManager;
import soot.jimple.infoflow.data.Abstraction;

public class NullFlowSensitivityUnitManager implements IFlowSensitivityUnitManager {
    @Override
    public Abstraction registerCallSite(Unit callSite, SootMethod callee, Abstraction activationAbs, Abstraction prev) {
        return activationAbs;
    }

    @Override
    public boolean pathFromCallToUnitExists(Unit callSite, Unit flowUnit) {
        return false;
    }
}
