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
     *
     *
     * @param callSite
     * @param activationUnit
     * @return true if the taint matches the call site
     */
    boolean isCallSiteActivatingTaint(Unit callSite, Unit activationUnit);
}
