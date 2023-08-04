package soot.jimple.infoflow.collections.operations.forward;

import java.util.Set;

import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.util.Callback;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.solver.IFollowReturnsPastSeedsHandler;
import soot.util.ConcurrentHashMultiMap;
import soot.util.MultiMap;

/**
 * FRPS handler responsible to inject the results of the lambdas back into the taint analysis
 *
 * @author Tim Lange
 */
public class ComputeFRPSHandler implements IFollowReturnsPastSeedsHandler {
    private final InfoflowManager manager;
    private final MultiMap<Pair<Abstraction, SootMethod>, Callback> callbacks;

    public ComputeFRPSHandler(InfoflowManager manager) {
        this.manager = manager;
        this.callbacks = new ConcurrentHashMultiMap<>();
    }

    public void registerCallback(Abstraction d1, Stmt stmt, Abstraction curr, Abstraction queued, SootMethod sm) {
        callbacks.put(new Pair<>(queued, sm), new Callback(d1, stmt, curr));
    }

    @Override
    public void handleFollowReturnsPastSeeds(Abstraction d1, Unit u, Abstraction d2) {
        if (!(u instanceof ReturnStmt))
            return;

        Value retOp = ((ReturnStmt) u).getOp();
        if (!manager.getAliasing().mayAlias(d2.getAccessPath().getPlainValue(), retOp))
            return;

        SootMethod sm = manager.getICFG().getMethodOf(u);
        Set<Callback> pairs = callbacks.get(new Pair<>(d1, sm));
        if (pairs == null)
            return;

        for (Callback p : pairs) {
            for (Unit s : manager.getICFG().getSuccsOf(p.getUnit())) {
                PathEdge<Unit, Abstraction> e = new PathEdge<>(p.getD1(), s, p.getD2());
                manager.getMainSolver().processEdge(e);
            }
        }
    }
}
