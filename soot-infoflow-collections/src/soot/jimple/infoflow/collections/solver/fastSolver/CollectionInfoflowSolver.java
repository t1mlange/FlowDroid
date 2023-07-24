package soot.jimple.infoflow.collections.solver.fastSolver;

import heros.SynchronizedBy;
import soot.Unit;
import soot.jimple.infoflow.collections.strategies.subsuming.SubsumingStrategy;
import soot.jimple.infoflow.collections.strategies.widening.WideningStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.fastSolver.InfoflowSolver;

public abstract class CollectionInfoflowSolver extends InfoflowSolver {
    @SynchronizedBy("Thread-safe class")
    protected WideningStrategy<Unit, Abstraction> widening;

    public CollectionInfoflowSolver(AbstractInfoflowProblem problem, InterruptableExecutor executor) {
        super(problem, executor);
    }

    public void setWideningStrategy(WideningStrategy<Unit, Abstraction> widening) {
        this.widening = widening;
    }
}
