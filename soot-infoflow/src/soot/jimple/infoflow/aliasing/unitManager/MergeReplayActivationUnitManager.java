package soot.jimple.infoflow.aliasing.unitManager;

import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.solver.IncomingRecord;
import soot.util.ConcurrentHashMultiMap;
import soot.util.MultiMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MergeReplayActivationUnitManager extends DefaultActivationUnitManager implements IMergeReplay {
    // Keeps track of all symbolic activation units
    private final Map<SymbolicActivationUnit, SymbolicActivationUnit> symbolSet = new ConcurrentHashMap<>();

    // Maps already concretized symbolic units to their corresponding call edge. Newly found concrete activation
    // unit have to be reinjected at these to prevent races between concretization and symbolization
    private final MultiMap<SymbolicActivationUnit, IncomingRecord<Unit, Abstraction>> symbolicIncoming = new ConcurrentHashMultiMap<>();

    public MergeReplayActivationUnitManager(InfoflowManager manager) {
        super(manager);
    }

    private Abstraction symbolize(Unit callSite, SootMethod callee,
                                  Abstraction exitAbs, Abstraction retSiteAbs) {
        // ASSERT: backward only
        assert !retSiteAbs.isAbstractionActive();

        SootMethod caller = manager.getICFG().getMethodOf(callSite);
        SymbolicActivationUnit newSym = new SymbolicActivationUnit(caller, callee, exitAbs.getActiveCopy());
        // Look up if a symbolic unit already exists for this return edge
        SymbolicActivationUnit sym = symbolSet.computeIfAbsent(newSym, (v) -> newSym);

        Stmt concreteActivationUnit = (Stmt) retSiteAbs.getActivationUnit();
        if (sym.addConcreteUnit(concreteActivationUnit)) {
            // If the symbolic activation unit already has been concretized, we need to reinject
            // the newly found concrete activation unit there too
            Collection<Unit> startPointsOf = manager.getICFG().getStartPointsOf(callee);
            for (IncomingRecord<Unit, Abstraction> inc : symbolicIncoming.get(sym)) {
                Abstraction d3a = inc.d3.replaceActivationUnit(concreteActivationUnit);

                if (!manager.getMainSolver().addIncoming(callee, d3a, inc.n, inc.d1, inc.d2))
                    continue;

                for (Unit sP : startPointsOf)
                    manager.getMainSolver().processEdge(new PathEdge<>(d3a, sP, d3a));
            }
        }

        // Propagate symbolic unit
        return retSiteAbs.replaceActivationUnit(sym);
    }

    @Override
    public Abstraction useGlobalUnit(Abstraction d3) {
        if (d3.getActivationUnit() == SymbolicActivationUnit.GAU)
            return d3;
        return d3.replaceActivationUnit(SymbolicActivationUnit.GAU);
    }

    @Override
    public Set<Abstraction> concretize(Abstraction d1, Unit callSite, Abstraction d2,
                                       SootMethod callee, Abstraction d3, boolean solverId) {
        if (!solverId) /* == alias */ {
            return Collections.singleton(useGlobalUnit(d3));
        } else {
            // TODO: leave this for the caller?
            if (d3.isAbstractionActive())
                return Collections.singleton(d3);

            Unit activationUnit = d3.getActivationUnit();
            // TODO: shouldn't the concrete unit be symbolized again?
            if (activationUnit == SymbolicActivationUnit.GAU
                    || !(activationUnit instanceof SymbolicActivationUnit))
                return Collections.singleton(d3);

            SymbolicActivationUnit sym = (SymbolicActivationUnit) activationUnit;
            SootMethod caller = manager.getICFG().getMethodOf(callSite);
            if (!sym.matchesContext(caller, callee))
                return Collections.singleton(d3.replaceActivationUnit(SymbolicActivationUnit.GAU));

            symbolicIncoming.put(sym, new IncomingRecord<>(callSite, d1, d2, d3));
            return sym.getConcreteUnits().stream()
                    .map(au -> d3.replaceActivationUnit((Stmt) au))
                    .collect(Collectors.toSet());
        }
    }

    // TODO: attatchActivationUnit in their artifact seems wrong???

    @Override
    public Abstraction registerCallSite(Unit callSite, SootMethod callee, Abstraction activationAbs, Abstraction prev) {
        assert !activationAbs.isAbstractionActive(); // We only register call sites backward????

        Unit activationUnit = activationAbs.getActivationUnit();
        // The global activation unit abstracts the flows in the backward analysis
        if (activationUnit == SymbolicActivationUnit.GAU)
            return activationAbs;

        super.registerCallSite(callSite, callee, activationAbs, prev);
        return symbolize(callSite, callee, activationAbs, prev);
    }
}
