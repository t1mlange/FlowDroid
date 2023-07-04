package soot.jimple.infoflow.solver.sparseJumpFunctions;

import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.fastSolver.IFDSSolver;
import soot.jimple.infoflow.solver.fastSolver.ISchedulingStrategy;

public class MemorySavingSchedulingStrategy {

    protected final MemorySavingInfoflowSolver solver;
    protected final IInfoflowCFG iCfg;

    /**
     * Strategy that schedules each edge individually, potentially in a new thread
     */
    public final MYSCHED EACH_EDGE_INDIVIDUALLY = new MYSCHED();

    class MYSCHED implements ISchedulingStrategy<Unit, Abstraction> {

        @Override
        public void propagateInitialSeeds(Abstraction sourceVal, Unit target, Abstraction targetVal, Unit relatedCallSite,
                                          boolean isUnbalancedReturn) {
            solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
                    IFDSSolver.ScheduleTarget.EXECUTOR, true);
        }

        @Override
        public void propagateNormalFlow(Abstraction sourceVal, Unit target, Abstraction targetVal, Unit relatedCallSite,
                                        boolean isUnbalancedReturn) {
            throw new RuntimeException("XXX");
        }

        public void propagateNormalFlow(Abstraction sourceVal, Unit target, Abstraction targetVal, Unit relatedCallSite,
                                        boolean isUnbalancedReturn, boolean isIdentityFlow) {
            solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
                    IFDSSolver.ScheduleTarget.EXECUTOR, !isIdentityFlow || iCfg.getPredsOf(target).size() != 1);
        }

        @Override
        public void propagateCallFlow(Abstraction sourceVal, Unit target, Abstraction targetVal, Unit relatedCallSite,
                                      boolean isUnbalancedReturn) {
            solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
                    IFDSSolver.ScheduleTarget.EXECUTOR, true);
        }

        @Override
        public void propagateCallToReturnFlow(Abstraction sourceVal, Unit target, Abstraction targetVal, Unit relatedCallSite,
                                              boolean isUnbalancedReturn) {
            solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
                    IFDSSolver.ScheduleTarget.EXECUTOR, true);
        }

        @Override
        public void propagateReturnFlow(Abstraction sourceVal, Unit target, Abstraction targetVal, Unit relatedCallSite,
                                        boolean isUnbalancedReturn) {
            solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
                    IFDSSolver.ScheduleTarget.EXECUTOR, true);
        }
    };

    public MemorySavingSchedulingStrategy(MemorySavingInfoflowSolver solver) {
        this.solver = solver;
        this.iCfg = solver.getTabulationProblem().interproceduralCFG();
    }
}
