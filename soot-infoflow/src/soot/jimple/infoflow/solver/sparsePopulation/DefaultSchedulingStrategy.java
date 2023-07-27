package soot.jimple.infoflow.solver.sparsePopulation;

import soot.SootMethod;
import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;
import soot.jimple.infoflow.solver.fastSolver.ISchedulingStrategy;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.infoflow.solver.sparsePopulation.IFDSSolver.ScheduleTarget;

/**
 * Default implementations for scheduling strategies
 * 
 * @author Steven Arzt
 *
 */
public class DefaultSchedulingStrategy<N, D extends FastSolverLinkedNode<D, N>, I extends BiDiInterproceduralCFG<N, SootMethod>> {

	protected final IFDSSolver<N, D, I> solver;

	/**
	 * Strategy that schedules each edge individually, potentially in a new thread
	 */
	public final ISchedulingStrategy<N, D> EACH_EDGE_INDIVIDUALLY = new ISchedulingStrategy<N, D>() {

		@Override
		public void propagateInitialSeeds(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
					ScheduleTarget.EXECUTOR, true);
		}

		@Override
		public void propagateNormalFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
										boolean isUnbalancedReturn, boolean isIdentityFlow) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
					ScheduleTarget.EXECUTOR, !isIdentityFlow || solver.icfg.getPredsOf(target).size() > 1);
		}

		@Override
		public void propagateCallFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
					ScheduleTarget.EXECUTOR, true);
		}

		@Override
		public void propagateCallToReturnFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
					ScheduleTarget.EXECUTOR, true);
		}

		@Override
		public void propagateReturnFlow(D sourceVal, N target, D targetVal, N relatedCallSite,
				boolean isUnbalancedReturn) {
			solver.propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn,
					ScheduleTarget.EXECUTOR, true);
		};

	};

	/**
	 * Creates a new instance of the {@link DefaultSchedulingStrategy} class
	 * 
	 * @param solver The solver on which to schedule the edges
	 */
	public DefaultSchedulingStrategy(IFDSSolver<N, D, I> solver) {
		this.solver = solver;
	}

}
