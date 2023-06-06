package soot.jimple.infoflow.collections.solver.fastSolver;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import heros.DontSynchronize;
import heros.FlowFunction;
import heros.SynchronizedBy;
import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.collections.strategies.subsuming.SubsumingStrategy;
import soot.jimple.infoflow.collections.strategies.widening.WideningStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.fastSolver.InfoflowSolver;
import soot.jimple.infoflow.solver.fastSolver.LocalWorklistTask;

/**
 * Infoflow Solver that supports various optimizations for precisely tracking collection keys/indices
 *
 * @author Tim Lange
 */
public class CollectionInfoflowSolver extends InfoflowSolver {
	@SynchronizedBy("Thread-safe class")
	private WideningStrategy<Unit, Abstraction> widening;

	@DontSynchronize("Read-only")
	private SubsumingStrategy<Abstraction> subsuming;

	@SynchronizedBy("Thread-safe data structure")
	protected MyConcurrentHashMap<Pair<Unit, Abstraction>, ConcurrentHashSet<PathEdge<Unit, Abstraction>>> jumpFunctionsForSubsuming = new MyConcurrentHashMap<>();

	// Collects all
	@SynchronizedBy("consistent lock on field")
	protected final MyConcurrentHashMap<Pair<SootMethod, Abstraction>, MyConcurrentHashMap<Unit, Map<Abstraction, Abstraction>>> subIncoming = new MyConcurrentHashMap<>();

	public CollectionInfoflowSolver(AbstractInfoflowProblem problem, InterruptableExecutor executor) {
		super(problem, executor);
		this.widening = null;
		this.subsuming = null;
	}

	public void setWideningStrategy(WideningStrategy<Unit, Abstraction> widening) {
		this.widening = widening;
	}

	public void setSubsuming(SubsumingStrategy<Abstraction> subsuming) {
		this.subsuming = subsuming;
	}

	/**
	 * Returns true if incoming already contains the queried edge
	 */
	protected boolean isElementOfIncoming(SootMethod m, Abstraction d3, Unit n, Abstraction d1, Abstraction d2) {
		MyConcurrentHashMap<Unit, Map<Abstraction, Abstraction>> summaries = incoming.get(new Pair<>(m, d3));
		if (summaries == null)
			return false;

		Map<Abstraction, Abstraction> set = summaries.get(n);
		if (set == null)
			return false;

		return set.containsKey(d1);
	}

	@Override
	protected void processCall(PathEdge<Unit, Abstraction> edge) {
		final Abstraction d1 = edge.factAtSource();
		final Unit n = edge.getTarget(); // a call node; line 14...

		final Abstraction d2Prev = edge.factAtTarget();
		assert d2Prev != null;

		// Widen if needed
		final Abstraction d2 = widening == null ? d2Prev : widening.widen(d2Prev, n);

		Collection<Unit> returnSiteUnits = icfg.getReturnSitesOfCallAt(n);

		// for each possible callee
		Collection<SootMethod> callees = icfg.getCalleesOfCallAt(n);
		if (callees != null && !callees.isEmpty()) {
			if (maxCalleesPerCallSite < 0 || callees.size() <= maxCalleesPerCallSite) {
				for (SootMethod sCalledProcUnit : callees) {
					if (!sCalledProcUnit.isConcrete())
						continue;
					// Early termination check
					if (killFlag != null)
						return;

					// compute the call-flow function
					FlowFunction<Abstraction> function = flowFunctions.getCallFlowFunction(n, sCalledProcUnit);
					Set<Abstraction> res = computeCallFlowFunction(function, d1, d2);

					if (res != null && !res.isEmpty()) {
						Collection<Unit> startPointsOf = icfg.getStartPointsOf(sCalledProcUnit);
						// for each result node of the call-flow function
						for (Abstraction d3 : res) {
							if (memoryManager != null)
								d3 = memoryManager.handleGeneratedMemoryObject(d2, d3);
							if (d3 == null)
								continue;

							// for each callee's start point(s)
							for (Unit sP : startPointsOf) {
								// create initial self-loop
								schedulingStrategy.propagateCallFlow(d3, sP, d3, n, false); // line 15
							}

							if (isElementOfIncoming(sCalledProcUnit, d3, n, d1, d2)) {
								// We have a direct match for that, so we can use the default summary
								applyEndSummaryOnCall(d1, n, d2, returnSiteUnits, sCalledProcUnit, d3);
								continue;
							}

							// register the fact that <sp,d3> has an incoming edge from
							// <n,d2>
							// line 15.1 of Unitaeem/Lhotak/Rodriguez
							if (!addIncoming(sCalledProcUnit, d3, n, d1, d2))
								continue;

							applyEndSummaryOnCall(d1, n, d2, returnSiteUnits, sCalledProcUnit, d3);
						}
					}
				}
			}
		}

		// line 17-19 of Unitaeem/Lhotak/Rodriguez
		// process intra-procedural flows along call-to-return flow functions
		for (Unit returnSiteUnit : returnSiteUnits) {
			FlowFunction<Abstraction> callToReturnFlowFunction = flowFunctions.getCallToReturnFlowFunction(n,
					returnSiteUnit);
			Set<Abstraction> res = computeCallToReturnFlowFunction(callToReturnFlowFunction, d1, d2);
			if (res != null && !res.isEmpty()) {
				for (Abstraction d3 : res) {
					if (memoryManager != null)
						d3 = memoryManager.handleGeneratedMemoryObject(d2, d3);
					if (d3 != null) {
						// Add result to check for widening
						if (widening != null)
							widening.recordNewFact(d3, n);

						schedulingStrategy.propagateCallToReturnFlow(d1, returnSiteUnit, d3, n, false);
					}
				}
			}
		}
	}

	protected class ComparablePathEdgeProcessingTask extends PathEdgeProcessingTask implements Comparable<ComparablePathEdgeProcessingTask> {
		public ComparablePathEdgeProcessingTask(PathEdge<Unit, Abstraction> edge, boolean solverId) {
			super(edge, solverId);
		}

		@Override
		public int compareTo(ComparablePathEdgeProcessingTask other) {
			return subsuming.compare(this.edge.factAtTarget(), other.edge.factAtTarget());
		}
	}

	@Override
	protected void scheduleEdgeProcessing(PathEdge<Unit, Abstraction> edge, ScheduleTarget scheduleTarget) {
		// If the executor has been killed, there is little point
		// in submitting new tasks
		if (killFlag != null || executor.isTerminating() || executor.isTerminated())
			return;

		ComparablePathEdgeProcessingTask task = new ComparablePathEdgeProcessingTask(edge, solverId);
		if (scheduleTarget == ScheduleTarget.EXECUTOR)
			executor.execute(task);
		else {
			LocalWorklistTask.scheduleLocal(task);
		}
		propagationCount++;
	}

	@Override
	protected void propagate(Abstraction sourceVal, Unit target, Abstraction targetVal,
			/* deliberately exposed to clients */ Unit relatedCallSite,
			/* deliberately exposed to clients */ boolean isUnbalancedReturn, ScheduleTarget scheduleTarget) {
		// Let the memory manager run
		if (memoryManager != null) {
			sourceVal = memoryManager.handleMemoryObject(sourceVal);
			targetVal = memoryManager.handleMemoryObject(targetVal);
			if (targetVal == null)
				return;
		}

		// Check the path length
		if (maxAbstractionPathLength >= 0 && targetVal.getPathLength() > maxAbstractionPathLength)
			return;

		final PathEdge<Unit, Abstraction> edge = new PathEdge<>(sourceVal, target, targetVal);
		final Abstraction existingVal = addFunction(edge);
		if (existingVal != null) {
			if (existingVal != targetVal) {
				// Check whether we need to retain this abstraction
				boolean isEssential;
				if (memoryManager == null)
					isEssential = relatedCallSite != null && icfg.isCallStmt(relatedCallSite);
				else
					isEssential = memoryManager.isEssentialJoinPoint(targetVal, relatedCallSite);

				if (maxJoinPointAbstractions < 0 || existingVal.getNeighborCount() < maxJoinPointAbstractions
						|| isEssential) {
					existingVal.addNeighbor(targetVal);
				}
			}
		} else {
			scheduleEdgeProcessing(edge, scheduleTarget);
		}
	}
}
