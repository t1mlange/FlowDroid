package soot.jimple.infoflow.aliasing.unitManager;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.aliasing.IFlowSensitivityUnitManager;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Set;

public class MergeTurnUnitManager implements IMergeReplay, IFlowSensitivityUnitManager {
    private static final DummyUnit placeholder = new DummyUnit();

    public void onCallFlow(Abstraction abs) {
        // Replace concrete turn unit with actual turn unit
        abs.deriveNewAbstractionWithTurnUnit(placeholder);
    }

    public void onReturnFlow(Abstraction abs) {
        abs.deriveNewAbstractionWithTurnUnit(placeholder);
        // replace
    }

    @Override
    public Abstraction attachActivationUnit(Abstraction retAbs, Abstraction callAbs) {
        return null;
    }

    @Override
    public Set<Abstraction> concretize(Abstraction d1, Unit callSite, Abstraction d2, SootMethod callee, Abstraction d3, boolean solverId) {
        return null;
    }

    @Override
    public Abstraction registerCallSite(Unit callSite, SootMethod callee, Abstraction activationAbs, Abstraction prev) {
        return null;
    }

    @Override
    public boolean pathFromCallToUnitExists(Unit callSite, Unit flowUnit) {
        return false;
    }
}
