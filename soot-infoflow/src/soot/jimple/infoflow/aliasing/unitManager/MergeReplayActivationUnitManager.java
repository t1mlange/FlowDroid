package soot.jimple.infoflow.aliasing.unitManager;

import heros.solver.PathEdge;
import soot.G;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.solver.IncomingRecord;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.util.ConcurrentHashMultiMap;
import soot.util.MultiMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MergeReplayActivationUnitManager extends DefaultActivationUnitManager implements IMergeReplay {
    // Keeps track of all symbolic activation units, Symb2Reps in the papere
    private final Map<SymbolicActivationUnit, SymbolicActivationUnit> symbolSet = new ConcurrentHashMap<>();

    // Maps already concretized symbolic units to their corresponding call edge. Newly found concrete activation
    // unit have to be reinjected at these to prevent races between concretization and symbolization
    private final MultiMap<SymbolicActivationUnit, IncomingRecord<Unit, Abstraction>> symbolicIncoming = new ConcurrentHashMultiMap<>();

    public MergeReplayActivationUnitManager(InfoflowManager manager) {
        super(manager);
    }

    private void onActivationUnitAdded(SymbolicActivationUnit sym, Unit activationUnit, SootMethod callee) {
        // If the symbolic activation unit already has been concretized, we need to reinject
        // the newly found concrete activation unit there too
        Collection<Unit> startPointsOf = manager.getICFG().getStartPointsOf(callee);
        for (IncomingRecord<Unit, Abstraction> inc : symbolicIncoming.get(sym)) { // ln 63
            Abstraction d3a = inc.d3.replaceActivationUnit(activationUnit); // ln 64

            if (!manager.getMainSolver().addIncoming(callee, d3a, inc.n, inc.d1, inc.d2))
                continue;

            for (Unit sP : startPointsOf)
                manager.getMainSolver().processEdge(new PathEdge<>(d3a, sP, d3a)); // ln 65

            manager.getMainSolver().applySummary(callee, d3a, inc.n, inc.d2, inc.d1); // ln 67-68
        }
    }

    private Abstraction symbolize(Unit callSite, SootMethod callee,
                                  Abstraction exitAbs, Abstraction retSiteAbs) {
        // Symbolize only happens backwards
        assert !retSiteAbs.isAbstractionActive();

        SootMethod caller = manager.getICFG().getMethodOf(callSite);
        Unit activationUnit = retSiteAbs.getActivationUnit(); // ln 71
        Abstraction abs = exitAbs.getActiveCopy(); // ln 72
        SymbolicActivationUnit newSym = new SymbolicActivationUnit(caller, callee, abs); // ln 73
        // Look up if a symbolic unit already exists for this return edge
        SymbolicActivationUnit sym = symbolSet.computeIfAbsent(newSym, (v) -> newSym); // ln 74-75
        // no need to reinject concrete's if the activation is already known
        if (sym.addUnit(activationUnit))
            onActivationUnitAdded(sym, activationUnit, callee); // ln 76
        // Propagate symbolic unit
        return retSiteAbs.replaceActivationUnit(sym); // ln 77
    }

    @Override
    public Set<Abstraction> concretize(Abstraction d1, Unit callSite, Abstraction d2,
                                       SootMethod callee, Abstraction d3, boolean solverId) {
        if (!solverId) /* == alias solver */ {
            assert !d3.isAbstractionActive();
            // line 38b: replace any symbolic activation unit with the global activation unit at return sites
            if (d3.getActivationUnit() == SymbolicActivationUnit.GAU)
                return Collections.singleton(d3);
            return Collections.singleton(d3.deriveSymbolicAbstraction(SymbolicActivationUnit.GAU));
        } else /* == main solver */ {
            if (d3.isAbstractionActive()) // ln 79
                return Collections.singleton(d3);

            Unit activationUnit = d3.getActivationUnit();
            if (activationUnit == SymbolicActivationUnit.GAU
                    || !(activationUnit instanceof SymbolicActivationUnit)) // ln 81
                return Collections.singleton(d3);

            SymbolicActivationUnit sym = (SymbolicActivationUnit) activationUnit;
            SootMethod caller = manager.getICFG().getMethodOf(callSite);
            if (!sym.matchesContext(caller, callee)) // ln 86
                return Collections.singleton(d3.deriveSymbolicAbstraction(SymbolicActivationUnit.GAU)); // ln 90

            symbolicIncoming.put(sym, new IncomingRecord<>(callSite, d1, d2, d3)); // ln 87
            return sym.getUnits().stream()
                    .map(d3::replaceActivationUnit)
                    .collect(Collectors.toSet()); // ln 88-89
        }
    }

    @Override
    public Abstraction attachActivationUnit(Abstraction retAbs, Abstraction callAbs) {
        if (!retAbs.isAbstractionActive()) {
            if (retAbs.getActivationUnit() == SymbolicActivationUnit.GAU && callAbs != null)
                return retAbs.deriveConcreteAbstraction(callAbs);
            if (retAbs.getActivationUnit() != SymbolicActivationUnit.GAU)
                return retAbs.replaceActivationUnit(SymbolicActivationUnit.GAU);
        }
        return retAbs;
    }

    @Override
    public Abstraction registerCallSite(Unit callSite, SootMethod callee, Abstraction activationAbs, Abstraction prev) {
        assert !activationAbs.isAbstractionActive(); // We only register call sites backward

        // The global activation unit abstracts the flows in the backward analysis
        Unit unit = getFlowUnit(activationAbs);
        if (unit == null || unit == SymbolicActivationUnit.GAU)
            return activationAbs;

        CallSite callSites = unitsToCallSites.computeIfAbsent(unit, (u) -> new CallSite());
        if (callSites.containsCallSite(callSite)) {
            Abstraction abs = symbolize(callSite, callee, activationAbs, prev);
            callSites.addCallsite(abs.getActivationUnit());
            return abs;
        }

        if (callSite instanceof SymbolicActivationUnit) {
            IInfoflowCFG icfg = manager.getICFG();
            if (!callSites.containsCallSiteMethod(callee))
                return symbolize(callSite, callee, activationAbs, prev);

            callSites.addCallsite(callSite, icfg);
            Abstraction abs = symbolize(callSite, callee, activationAbs, prev);
            callSites.addCallsite(abs.getActivationUnit());
            return activationAbs;
        } else {
            IInfoflowCFG icfg = manager.getICFG();
            if (!callee.getActiveBody().getUnits().contains(unit)
                    && !callSites.containsCallSiteMethod(callee))
                return symbolize(callSite, callee, activationAbs, prev);

            callSites.addCallsite(callSite, icfg);
            Abstraction abs = symbolize(callSite, callee, activationAbs, prev);
            callSites.addCallsite(abs.getActivationUnit());
            return activationAbs;
        }
    }
}
