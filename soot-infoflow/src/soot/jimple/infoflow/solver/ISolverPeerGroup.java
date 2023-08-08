package soot.jimple.infoflow.solver;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Set;

/**
 * A solver peer group contains solvers that operate on the same target and are interconnected between each other.
 * The solver peer group is responsible to track the data flows
 */
public interface ISolverPeerGroup {
    /**
     * Add a solver to this peer group and notify the solver that it joined this peer group
     *
     * @param solver IFDS solver
     */
    void addSolver(IInfoflowSolver solver);

    /**
     * Gets the set of incoming abstractions
     *
     * @param d1 calling context
     * @param m  method
     * @return   set of incoming abstractions
     */
    Set<IncomingRecord<Unit, Abstraction>> incoming(Abstraction d1, SootMethod m);

    /**
     * Add a new record to the incoming set of a method
     *
     * @param m   method
     * @param n   call site
     * @param d3a
     * @param d1  calling context in caller
     * @param d2  incoming abstraction at call site
     * @param d3  calling context
     * @return true if the added record is new
     */
    boolean addIncoming(SootMethod m, Unit n, Abstraction d3a, Abstraction d1, Abstraction d2, Abstraction d3);
}
