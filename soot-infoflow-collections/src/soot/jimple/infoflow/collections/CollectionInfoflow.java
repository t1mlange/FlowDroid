package soot.jimple.infoflow.collections;

import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.problems.rules.CollectionRulePropagationManagerFactory;
import soot.jimple.infoflow.collections.solver.fastSolver.AppendingCollectionInfoflowSolver;
import soot.jimple.infoflow.collections.solver.fastSolver.WideningCollectionInfoflowSolver;
import soot.jimple.infoflow.collections.strategies.appending.DefaultAppendingStrategy;
import soot.jimple.infoflow.collections.strategies.widening.WideningOnRevisitStrategy;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.fastSolver.InfoflowSolver;

public class CollectionInfoflow extends Infoflow {
    public enum Solver {
        Widening,
        Appending,
    }

    private Solver solver;

    public CollectionInfoflow(String s, boolean b, DefaultBiDiICFGFactory defaultBiDiICFGFactory) {
        super(s, b, defaultBiDiICFGFactory);
    }

    public CollectionInfoflow withSolver(Solver solver) {
        this.solver = solver;
        return this;
    }

    @Override
    protected IPropagationRuleManagerFactory initializeRuleManagerFactory() {
        return new CollectionRulePropagationManagerFactory();
    }

    @Override
    protected IInfoflowSolver createDataFlowSolver(InterruptableExecutor executor, AbstractInfoflowProblem problem,
                                                   InfoflowConfiguration.SolverConfiguration solverConfig) {
        if (solver == null)
            return super.createDataFlowSolver(executor, problem, solverConfig);

        switch (solver) {
            case Widening:
                WideningCollectionInfoflowSolver wideningSolver = new WideningCollectionInfoflowSolver(problem, executor);
                wideningSolver.setWideningStrategy(new WideningOnRevisitStrategy(manager));
                solverPeerGroup.addSolver(wideningSolver);
                return wideningSolver;
            case Appending:
                AppendingCollectionInfoflowSolver appendingSolver = new AppendingCollectionInfoflowSolver(problem, executor);
                appendingSolver.setAppendingStrategy(new DefaultAppendingStrategy(manager));
                solverPeerGroup.addSolver(appendingSolver);
                return appendingSolver;
            default:
                return super.createDataFlowSolver(executor, problem, solverConfig);
        }
    }
}
