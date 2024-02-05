package soot.jimple.infoflow.aliasing;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;

public interface IFlowSensitivityUnitManager {
    /**
     * Register call site for activation unit
     *
     * @param callSite      Current call site
     * @param callee        Callee of the call site
     * @param activationAbs Current abstraction
     * @param prev          Previous abstracton
     * @return abstraction that superseeeds activationAbs
     */
    Abstraction registerCallSite(Unit callSite, SootMethod callee, Abstraction activationAbs, Abstraction prev);

    /**
     * Checks if path from the call to the unit exists
     *
     * @param callSite
     * @param flowUnit
     * @return true if the taint matches the call site
     */
    boolean pathFromCallToUnitExists(Unit callSite, Unit flowUnit);
}
