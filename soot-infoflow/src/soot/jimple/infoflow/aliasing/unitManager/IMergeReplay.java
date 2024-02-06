package soot.jimple.infoflow.aliasing.unitManager;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Set;

public interface IMergeReplay {
    Abstraction attachActivationUnit(Abstraction retAbs, Abstraction callAbs);

    Set<Abstraction> concretize(Abstraction d1, Unit callSite, Abstraction d2, SootMethod callee, Abstraction d3, boolean solverId);
}
