package soot.jimple.infoflow.aliasing;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

import java.lang.ref.SoftReference;
import java.util.Set;

public class CallSites {
    private Set<Unit> callsites = new ConcurrentHashSet<>();
    private Set<SootMethod> callsiteMethods = new ConcurrentHashSet<>();

    public boolean containsCallSite(Unit callSite) {
        return callsites.contains(callSite);
    }

    public boolean containsCallSiteMethod(SootMethod callSite) {
        return callsiteMethods.contains(callSite);
    }

    public boolean addCallsite(Unit callSite, IInfoflowCFG icfg) {
        if (callsites.add(callSite)) {
            callsiteMethods.add(icfg.getMethodOf(callSite));
            return true;
        }
        return false;
    }
}
