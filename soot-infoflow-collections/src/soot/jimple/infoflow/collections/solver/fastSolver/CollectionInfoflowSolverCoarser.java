package soot.jimple.infoflow.collections.solver.fastSolver;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import heros.DontSynchronize;
import heros.FlowFunction;
import heros.SynchronizedBy;
import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.collections.strategies.subsuming.SubsumingStrategy;
import soot.jimple.infoflow.collections.strategies.widening.WideningStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.EndSummary;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.fastSolver.InfoflowSolver;
import soot.jimple.infoflow.solver.fastSolver.LocalWorklistTask;
import soot.util.ConcurrentHashMultiMap;
import soot.util.MultiMap;

/**
 * Infoflow Solver that supports various optimizations for precisely tracking collection keys/indices
 * - Supports widening on CallToReturn edges
 * - Applies coarser summaries when possible
 *
 * TODO: data races on processCall?
 *
 * @author Tim Lange
 */
public class CollectionInfoflowSolverCoarser extends CollectionInfoflowSolver {
	// We need to overwrite the default incoming, because we might have multiple elements per context
	// e.g. when we add a more precise collection taint to use the summary of the coarser collection taint
	@SynchronizedBy("Thread-safe data structure")
	protected final MyConcurrentHashMap<Pair<SootMethod, Abstraction>,
										MyConcurrentHashMap<Unit, MultiMap<Abstraction, Abstraction>>> myIncoming = new MyConcurrentHashMap<>();

	// Collects all incoming abstractions with a context and maps them to the possible summaries
	@SynchronizedBy("Thread-safe data structure")
	protected final MultiMap<NoContextKey, Abstraction> incomingWContext = new ConcurrentHashMultiMap<>();

	protected final MultiMap<NoContextKey, Abstraction> summariesWContext = new ConcurrentHashMultiMap<>();

	public CollectionInfoflowSolverCoarser(AbstractInfoflowProblem problem, InterruptableExecutor executor) {
		super(problem, executor);
	}

	protected boolean incomingContains(SootMethod m, Abstraction d3, Unit n, Abstraction d1, Abstraction d2) {
		MyConcurrentHashMap<Unit, MultiMap<Abstraction, Abstraction>> summaries
				= myIncoming.putIfAbsentElseGet(new Pair<>(m, d3), MyConcurrentHashMap::new);
		MultiMap<Abstraction, Abstraction> set = summaries.putIfAbsentElseGet(n, ConcurrentHashMultiMap::new);
		return set.contains(d1, d2);
	}


	protected boolean addIncomingButNoSummary(SootMethod m, Abstraction d3, Unit n, Abstraction d1, Abstraction d2) {
		MyConcurrentHashMap<Unit, MultiMap<Abstraction, Abstraction>> summaries
				= myIncoming.putIfAbsentElseGet(new Pair<>(m, d3), MyConcurrentHashMap::new);
		MultiMap<Abstraction, Abstraction> set = summaries.putIfAbsentElseGet(n, ConcurrentHashMultiMap::new);
		return set.put(d1, d2);
	}

	@Override
	protected boolean addIncoming(SootMethod m, Abstraction d3, Unit n, Abstraction d1, Abstraction d2) {
		MyConcurrentHashMap<Unit, MultiMap<Abstraction, Abstraction>> summaries
				= myIncoming.putIfAbsentElseGet(new Pair<>(m, d3), MyConcurrentHashMap::new);
		MultiMap<Abstraction, Abstraction> set = summaries.putIfAbsentElseGet(n, ConcurrentHashMultiMap::new);

		// Remember that we have seen a context-sensitive taint here
		incomingWContext.put(new NoContextKey(m, d3), d3);

		return set.put(d1, d2);
	}

	@Override
	protected Map<Unit, Map<Abstraction, Abstraction>> incoming(Abstraction d1, SootMethod m) {
		throw new RuntimeException("Use myIncoming(Abstraction, SootMethod) instead!");
	}

	protected Map<Unit, MultiMap<Abstraction, Abstraction>> myIncoming(Abstraction d1, SootMethod m) {
		return myIncoming.get(new Pair<>(m, d1));
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

							synchronized (this) {
								// Maybe we already have seen a coarser taint for this method
								if (subsuming != null && subsuming.hasContext(d3)) {
									if (incomingContains(sCalledProcUnit, d3, n, d1, d2)) {
										// If we were already here with the same (context, taint),
										// we can skip the rest
										continue;
									}

									// Incoming does not know this, maybe we have the same abstraction with a larger
									// context for this, so we can skip this callee
									Set<Abstraction> inc = incomingWContext.get(new NoContextKey(sCalledProcUnit, d3));
									// If we already know the abstraction, we have a context, so we don't need to search
									if (inc != null) {
										Abstraction smallestExistingSum = subsuming.chooseContext(inc, d3);
										if (smallestExistingSum != null) {
											// Choose less precise incoming set instead of reanalyzing the method
											addIncomingButNoSummary(sCalledProcUnit, smallestExistingSum, n, d1, d2);
											applyEndSummaryOnCall(d1, n, d2, returnSiteUnits, sCalledProcUnit, d3);

											// We have found a suiting coarser abstraction and thus, won't
											// propagate this taint further in the callee.
											continue;
										}
									}
								}
							}

							// register the fact that <sp,d3> has an incoming edge from
							// <n,d2>
							// line 15.1 of Naeem/Lhotak/Rodriguez
							if (!addIncoming(sCalledProcUnit, d3, n, d1, d2)) {
								// If we were already here with the same (context, taint),
								// we can skip the rest
								continue;
							}

							// for each callee's start point(s)
							for (Unit sP : startPointsOf) {
								// create initial self-loop
								schedulingStrategy.propagateCallFlow(d3, sP, d3, n, false); // line 15
							}

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

	@Override
	protected void applyEndSummaryOnCall(final Abstraction d1, final Unit n, final Abstraction d2, Collection<Unit> returnSiteNs,
										 SootMethod sCalledProcN, Abstraction d3) {
		// line 15.2
		Abstraction summaryQuery = d3;
		Set<EndSummary<Unit, Abstraction>> endSumm = endSummary(sCalledProcN, d3);

		// Maybe we can use coarser summary here
		if (endSumm == null || !endSumm.isEmpty()) {
			if (subsuming != null && subsuming.hasContext(d3)) {
				Set<Abstraction> sums = summariesWContext.get(new NoContextKey(sCalledProcN, d3));
				// If we already know the abstraction, we have a context, so we don't need to search
				if (sums != null) {
					Abstraction smallestExistingSum = subsuming.chooseContext(sums, d3);
					if (smallestExistingSum != null) {
						summaryQuery = smallestExistingSum;
						// Use less precise summary in favor of reanalyzing the method
						endSumm = endSummary(sCalledProcN, smallestExistingSum);
					}
				}
			}
		}

		// still line 15.2 of Naeem/Lhotak/Rodriguez
		// for each already-queried exit value <eP,d4> reachable
		// from <sP,d3>, create new caller-side jump functions to
		// the return sites because we have observed a potentially
		// new incoming edge into <sP,d3>
		if (endSumm != null && !endSumm.isEmpty()) {
			for (EndSummary<Unit, Abstraction> entry : endSumm) {
				Unit eP = entry.eP;
				Abstraction d4 = entry.d4;

				// Special case: If the summary is the identity, we want to prevent
				// the coarser summary from changing the caller-side abstraction.
				// Thus, we narrow the abstraction here.
				if (d4.equals(summaryQuery)) {
					d4 = d3;
				}

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
			Map<EndSummary<Unit, Abstraction>, EndSummary<Unit, Abstraction>> summaries
					= endSummary.putIfAbsentElseGet(new Pair<>(m, d1), ConcurrentHashMap::new);
			newSummary = new EndSummary<>(eP, d2, d1);
			EndSummary<Unit, Abstraction> existingSummary = summaries.putIfAbsent(newSummary, newSummary);
			if (existingSummary != null) {
				existingSummary.calleeD1.addNeighbor(d2);
				return false;
			}
		}

		// Register that we have a summary for this context-dependent taint
		if (subsuming != null && subsuming.hasContext(d1)) {
			summariesWContext.put(new NoContextKey(m, d1), d1);
		}

		return true;
	}

	private void processExitIFDSSolver(PathEdge<Unit, Abstraction> edge) {
		final Unit n = edge.getTarget(); // an exit node; line 21...
		SootMethod methodThatNeedsSummary = icfg.getMethodOf(n);

		final Abstraction d1 = edge.factAtSource();
		final Abstraction d2 = edge.factAtTarget();

		// for each of the method's start points, determine incoming calls

		// line 21.1 of Naeem/Lhotak/Rodriguez
		// register end-summary
		if (!addEndSummary(methodThatNeedsSummary, d1, n, d2))
			return;
		Map<Unit, MultiMap<Abstraction, Abstraction>> inc = myIncoming(d1, methodThatNeedsSummary);

		// for each incoming call edge already processed
		// (see processCall(..))
		if (inc != null && !inc.isEmpty())
			for (Map.Entry<Unit, MultiMap<Abstraction, Abstraction>> entry : inc.entrySet()) {
				// Early termination check
				if (killFlag != null)
					return;

				// line 22
				Unit c = entry.getKey();
				Set<Abstraction> callerSideDs = entry.getValue().keySet();
				// for each return site
				for (Unit retSiteC : icfg.getReturnSitesOfCallAt(c)) {
					// compute return-flow function
					FlowFunction<Abstraction> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary, n,
							retSiteC);
					Set<Abstraction> targets = computeReturnFlowFunction(retFunction, d1, d2, c, callerSideDs);
					// for each incoming-call value
					if (targets != null && !targets.isEmpty()) {
						for (final Abstraction d4 : entry.getValue().keySet()) {
							for (final Abstraction predVal : entry.getValue().get(d4)) {
								for (Abstraction d5 : targets) {
									if (memoryManager != null)
										d5 = memoryManager.handleGeneratedMemoryObject(d2, d5);
									if (d5 == null)
										continue;

									// If we have not changed anything in the callee, we do not need the facts from
									// there. Even if we change something: If we don't need the concrete path, we
									// can skip the callee in the predecessor chain
									Abstraction d5p = shortenPredecessors(d5, predVal, d1, n, c);
									schedulingStrategy.propagateReturnFlow(d4, retSiteC, d5p, c, false);
								}
							}
						}
					}
				}
			}

		// handling for unbalanced problems where we return out of a method with
		// a fact for which we have no incoming flow
		// note: we propagate that way only values that originate from ZERO, as
		// conditionally generated values should only be propagated into callers that
		// have an incoming edge for this condition
		if (followReturnsPastSeeds && d1 == zeroValue && (inc == null || inc.isEmpty())) {
			Collection<Unit> callers = icfg.getCallersOf(methodThatNeedsSummary);
			for (Unit c : callers) {
				for (Unit retSiteC : icfg.getReturnSitesOfCallAt(c)) {
					FlowFunction<Abstraction> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary, n,
							retSiteC);
					Set<Abstraction> targets = computeReturnFlowFunction(retFunction, d1, d2, c,
							Collections.singleton(zeroValue));
					if (targets != null && !targets.isEmpty()) {
						for (Abstraction d5 : targets) {
							if (memoryManager != null)
								d5 = memoryManager.handleGeneratedMemoryObject(d2, d5);
							if (d5 != null)
								schedulingStrategy.propagateReturnFlow(zeroValue, retSiteC, d5, c, true);
						}
					}
				}
			}
			// in cases where there are no callers, the return statement would
			// normally not be processed at all; this might be undesirable if the flow
			// function has a side effect such as registering a taint; instead we thus call
			// the return flow function will a null caller
			if (callers.isEmpty()) {
				FlowFunction<Abstraction> retFunction = flowFunctions.getReturnFlowFunction(null, methodThatNeedsSummary, n,
						null);
				retFunction.computeTargets(d2);
			}
		}
	}

	@Override
	protected void processExit(PathEdge<Unit, Abstraction> edge) {
		processExitIFDSSolver(edge);

		if (followReturnsPastSeeds && followReturnsPastSeedsHandler != null) {
			final Abstraction d1 = edge.factAtSource();
			final Unit u = edge.getTarget();
			final Abstraction d2 = edge.factAtTarget();

			final SootMethod methodThatNeedsSummary = icfg.getMethodOf(u);
			final Map<Unit, MultiMap<Abstraction, Abstraction>> inc = myIncoming(d1, methodThatNeedsSummary);

			if (inc == null || inc.isEmpty())
				followReturnsPastSeedsHandler.handleFollowReturnsPastSeeds(d1, u, d2);
		}
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
