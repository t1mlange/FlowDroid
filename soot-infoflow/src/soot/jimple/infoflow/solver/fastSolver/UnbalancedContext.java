package soot.jimple.infoflow.solver.fastSolver;

import soot.Type;
import soot.Unit;

public class UnbalancedContext implements FastSolverLinkedNode<UnbalancedContext, Unit> {
    public UnbalancedContext() {

    }

    @Override
    public boolean addNeighbor(UnbalancedContext originalAbstraction) {
        return false;
    }

    @Override
    public int getNeighborCount() {
        return 0;
    }

    @Override
    public void setPredecessor(UnbalancedContext predecessor) {

    }

    @Override
    public UnbalancedContext getPredecessor() {
        return null;
    }

    @Override
    public UnbalancedContext clone() {
        return null;
    }

    @Override
    public UnbalancedContext clone(Unit currentUnit, Unit callSite) {
        return null;
    }

    @Override
    public UnbalancedContext getActiveCopy() {
        return null;
    }

    @Override
    public int getPathLength() {
        return 0;
    }

    @Override
    public Type getType() {
        return null;
    }
}
