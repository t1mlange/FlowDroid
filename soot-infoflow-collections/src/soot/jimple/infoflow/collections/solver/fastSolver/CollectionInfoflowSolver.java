package soot.jimple.infoflow.collections.solver.fastSolver;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import soot.jimple.infoflow.collections.solver.fastSolver.executors.AbstractionWithoutContextKey;
import soot.jimple.infoflow.collections.strategies.subsuming.SubsumingStrategy;
import soot.jimple.infoflow.collections.strategies.widening.WideningStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.EndSummary;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.fastSolver.InfoflowSolver;
import soot.jimple.infoflow.solver.fastSolver.LocalWorklistTask;
import soot.util.MultiMap;

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

	protected final MyConcurrentHashMap<Pair<SootMethod, AbstractionWithoutContextKey>, Map<Abstraction, EndSummary<Unit, Abstraction>>> summariesWContext = new MyConcurrentHashMap<>();

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

							if (subsuming != null && subsuming.hasContext(d3)) {
								Map<Abstraction, EndSummary<Unit, Abstraction>> sums = summariesWContext.get(new Pair<>(sCalledProcUnit, new AbstractionWithoutContextKey(d3)));
								// If we already know the abstraction, we have a context, so we don't need to search
								if (sums != null && !sums.containsKey(d3)) {
									Abstraction smallestExistingSum = null;
									for (Abstraction sumD1 : sums.keySet()) {
										if (sumD1.entails(d3)) {
											// We want to use the most precise summary here
											if (smallestExistingSum == null || smallestExistingSum.entails(sumD1))
												smallestExistingSum = sumD1;
										}
									}

									if (smallestExistingSum == null) {
										Abstraction woCtxt = subsuming.removeContext(d3);
										if (endSummary.containsKey(new Pair<>(sCalledProcUnit, woCtxt)))
											smallestExistingSum = woCtxt;
									}

									if (smallestExistingSum != null) {
										applyEndSummaryOnCall(d1, n, d2, returnSiteUnits, sCalledProcUnit, d3, smallestExistingSum);
										continue;
									}
								}
							}

							// for each callee's start point(s)
							for (Unit sP : startPointsOf) {
								// create initial self-loop
								schedulingStrategy.propagateCallFlow(d3, sP, d3, n, false); // line 15
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


	protected void applyEndSummaryOnCall(final Abstraction d1, final Unit n, final Abstraction d2, Collection<Unit> returnSiteNs,
										 SootMethod sCalledProcN, Abstraction d3, Abstraction summaryLookup) {
		// line 15.2
		Set<EndSummary<Unit, Abstraction>> endSumm = endSummary(sCalledProcN, summaryLookup);

		// still line 15.2 of Naeem/Lhotak/Rodriguez
		// for each already-queried exit value <eP,d4> reachable
		// from <sP,d3>, create new caller-side jump functions to
		// the return sites because we have observed a potentially
		// new incoming edge into <sP,d3>
		if (endSumm != null && !endSumm.isEmpty()) {
			for (EndSummary<Unit, Abstraction> entry : endSumm) {
				Unit eP = entry.eP;
				Abstraction d4 = entry.d4;

				// We must acknowledge the incoming abstraction from the other path
				entry.calleeD1.addNeighbor(d3);

				// for each return site
				for (Unit retSiteN : returnSiteNs) {
					// compute return-flow function
					FlowFunction<Abstraction> retFunction = flowFunctions.getReturnFlowFunction(n, sCalledProcN, eP, retSiteN);
					Set<Abstraction> retFlowRes = computeReturnFlowFunction(retFunction, d3, d4, n, Collections.singleton(d1));
					if (retFlowRes != null && !retFlowRes.isEmpty()) {
						// for each target value of the function
						for (Abstraction d5 : retFlowRes) {
							if (memoryManager != null)
								d5 = memoryManager.handleGeneratedMemoryObject(d4, d5);

							// If we have not changed anything in
							// the callee, we do not need the facts from
							// there. Even if we change something:
							// If we don't need the concrete path,
							// we can skip the callee in the predecessor
							// chain
							Abstraction d5p = shortenPredecessors(d5, d2, d3, eP, n);
							schedulingStrategy.propagateReturnFlow(d1, retSiteN, d5p, n, false);
						}
					}
				}
			}
			onEndSummaryApplied(n, sCalledProcN, d3);
		}
	}

	@Override
	protected boolean addEndSummary(SootMethod m, Abstraction d1, Unit eP, Abstraction d2) {
		if (d1 == zeroValue)
			return true;

		EndSummary<Unit, Abstraction> newSummary;
		{
			Map<EndSummary<Unit, Abstraction>, EndSummary<Unit, Abstraction>> summaries = endSummary.putIfAbsentElseGet(new Pair<>(m, d1),
					() -> new ConcurrentHashMap<>());
			newSummary = new EndSummary<>(eP, d2, d1);
			EndSummary<Unit, Abstraction> existingSummary = summaries.putIfAbsent(newSummary, newSummary);
			if (existingSummary != null) {
				existingSummary.calleeD1.addNeighbor(d2);
				return false;
			}
		}

		if (subsuming != null && subsuming.hasContext(d1)) {
			Map<Abstraction, EndSummary<Unit, Abstraction>> summaries
					= summariesWContext.putIfAbsentElseGet(new Pair<>(m, new AbstractionWithoutContextKey(d1)), ConcurrentHashMap::new);
			summaries.put(d1, newSummary);
		}

		return true;
	}

	protected class ComparablePathEdgeProcessingTask extends PathEdgeProcessingTask implements Comparable<ComparablePathEdgeProcessingTask> {
		public ComparablePathEdgeProcessingTask(PathEdge<Unit, Abstraction> edge, boolean solverId) {
			super(edge, solverId);
		}

		@Override
		public int compareTo(ComparablePathEdgeProcessingTask other) {
			return subsuming == null ? 0 : subsuming.compare(this.edge.factAtTarget(), other.edge.factAtTarget());
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
}
