package soot.jimple.infoflow.collections;

import java.util.*;
import java.util.stream.Collectors;

import heros.DontSynchronize;
import heros.SynchronizedBy;
import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.*;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.CollectionMethod;
import soot.jimple.infoflow.collections.data.CollectionModel;
import soot.jimple.infoflow.collections.operations.ICollectionOperation;
import soot.jimple.infoflow.collections.operations.LocationDependentOperation;
import soot.jimple.infoflow.collections.operations.forward.AbstractShiftOperation;
import soot.jimple.infoflow.collections.solver.fastSolver.CollectionInfoflowSolver;
import soot.jimple.infoflow.collections.solver.fastSolver.CollectionInfoflowSolverCoarser;
import soot.jimple.infoflow.collections.strategies.containers.ConstantStrategy;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.collections.strategies.subsuming.LargerContextSubsumingStrategy;
import soot.jimple.infoflow.collections.strategies.subsuming.SubsumingStrategy;
import soot.jimple.infoflow.collections.strategies.widening.WideningOnRevisitStrategy;
import soot.jimple.infoflow.collections.strategies.widening.WideningStrategy;
import soot.jimple.infoflow.collections.util.AliasAbstractionSet;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.solver.IFollowReturnsPastSeedsHandler;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.util.ConcurrentHashMultiMap;
import soot.util.MultiMap;

/**
 * Taint Wrapper that models the behavior of collections w.r.t. keys or indices
 *
 * @author Tim Lange
 */
public class CollectionTaintWrapper implements ITaintPropagationWrapper {
	@SynchronizedBy("Read-only during analysis")
	private final Map<String, CollectionModel> models;

	@SynchronizedBy("Read-only during analysis")
	private final ITaintPropagationWrapper fallbackWrapper;

	@DontSynchronize("Benign race")
	private int wrapperHits;
	@DontSynchronize("Benign race")
	private int wrapperMisses;

	protected InfoflowManager manager;

	private IContainerStrategy strategy;

	public CollectionTaintWrapper(Map<String, CollectionModel> models, ITaintPropagationWrapper fallbackWrapper) {
		this.models = models;
		this.fallbackWrapper = fallbackWrapper;
		this.wrapperHits = 0;
		this.wrapperMisses = 0;
	}

	@Override
	public void initialize(InfoflowManager manager) {
		if (fallbackWrapper != null)
			fallbackWrapper.initialize(manager);

		this.manager = manager;

		this.strategy = getStrategy();
		manager.getMainSolver().setFollowReturnsPastSeedsHandler(new CollectionCallbackHandler());

		// Get all method subsignatures that may result in an infinite domain
		Set<String> subSigs = this.models.values().stream()
								.flatMap(model -> model.getAllMethods().stream())
								.filter(method -> Arrays.stream(method.operations())
													.anyMatch(o -> o instanceof AbstractShiftOperation))
								.map(CollectionMethod::getSubSignature) // get the subsig
								.collect(Collectors.toUnmodifiableSet());
		WideningStrategy<Unit, Abstraction> w = new WideningOnRevisitStrategy(manager, subSigs);
		Set<String> allSubSigs = this.models.values().stream()
				.flatMap(model -> model.getAllMethods().stream())
				.filter(method -> Arrays.stream(method.operations()).anyMatch(op -> op instanceof LocationDependentOperation))
				.map(CollectionMethod::getSubSignature) // get the subsig
				.collect(Collectors.toUnmodifiableSet());
		SubsumingStrategy<Unit, Abstraction> s = new LargerContextSubsumingStrategy(manager, allSubSigs);
		if (manager.getMainSolver() instanceof CollectionInfoflowSolver) {
			((CollectionInfoflowSolverCoarser) manager.getMainSolver()).setWideningStrategy(w);
			((CollectionInfoflowSolverCoarser) manager.getMainSolver()).setSubsuming(s);
		}
		if (manager.getAliasSolver() instanceof CollectionInfoflowSolver) {
			((CollectionInfoflowSolverCoarser) manager.getAliasSolver()).setWideningStrategy(w);
			((CollectionInfoflowSolverCoarser) manager.getAliasSolver()).setSubsuming(s);
		}
	}

	@Override
	public Collection<PreAnalysisHandler> getPreAnalysisHandlers() {
		if (fallbackWrapper != null)
			return fallbackWrapper.getPreAnalysisHandlers();

		return Collections.emptyList();
	}

	protected IContainerStrategy getStrategy() {
		return new ConstantStrategy(manager, this);
	}

	private Set<Abstraction> fallbackTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
		if (fallbackWrapper != null)
			return fallbackWrapper.getTaintsForMethod(stmt, d1, taintedPath);

		wrapperMisses++;
		return null;
	}

	private static class Callback {
		Abstraction d1;
		Abstraction d2;
		Unit u;

		public Callback(Abstraction d1, Unit u, Abstraction d2) {
			this.d1 = d1;
			this.u = u;
			this.d2 = d2;
		}
	}

	private class CollectionCallbackHandler implements IFollowReturnsPastSeedsHandler {
		@Override
		public void handleFollowReturnsPastSeeds(Abstraction d1, Unit u, Abstraction d2) {
			if (u instanceof ReturnStmt) {
				Value retOp = ((ReturnStmt) u).getOp();
				if (manager.getAliasing().mayAlias(d2.getAccessPath().getPlainValue(), retOp)) {
					SootMethod sm = manager.getICFG().getMethodOf(u);
					Set<Callback> pairs = callbacks.get(new Pair<>(d1, sm));
					if (pairs != null) {
						for (Callback p : pairs) {
							for (Unit s : manager.getICFG().getSuccsOf(p.u)) {
								PathEdge<Unit, Abstraction> e = new PathEdge<>(p.d1, s, p.d2);
								manager.getMainSolver().processEdge(e);
							}
						}
					}
				}
			}
		}
	}

	MultiMap<Pair<Abstraction, SootMethod>, Callback> callbacks = new ConcurrentHashMultiMap<>();

	public void registerCallback(Abstraction d1, Stmt stmt, Abstraction curr, Abstraction queued, SootMethod sm) {
		callbacks.put(new Pair<>(queued, sm), new Callback(d1, stmt, curr));
	}

	@Override
	public Set<Abstraction> getTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
		if (!stmt.containsInvokeExpr())
			return fallbackTaintsForMethod(stmt, d1, taintedPath);

		SootMethod sm = stmt.getInvokeExpr().getMethod();
		CollectionMethod method = getCollectionMethodForSootMethod(sm);
		if (method == null)
			return fallbackTaintsForMethod(stmt, d1, taintedPath);

		wrapperHits++;

		Set<Abstraction> outSet = new HashSet<>();
		boolean killCurrentTaint = false;
		for (ICollectionOperation op : method.operations()) {
			killCurrentTaint |= op.apply(d1, taintedPath, stmt, manager, strategy, outSet);
		}

		if (!killCurrentTaint)
			outSet.add(taintedPath);

		return outSet;
	}

	@Override
	public boolean isExclusive(Stmt stmt, Abstraction taintedPath) {
		if (supportsCallee(stmt))
			return true;

		if (fallbackWrapper != null)
			return fallbackWrapper.isExclusive(stmt, taintedPath);
		return false;
	}

	private Set<Abstraction> fallbackAliasForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
		if (fallbackWrapper != null)
			return fallbackWrapper.getAliasesForMethod(stmt, d1, taintedPath);

		wrapperMisses++;
		return null;
	}

	@Override
	public Set<Abstraction> getAliasesForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
		if (!stmt.containsInvokeExpr())
			return fallbackAliasForMethod(stmt, d1, taintedPath);

		SootMethod sm = stmt.getInvokeExpr().getMethod();
		CollectionMethod method = getCollectionMethodForSootMethod(sm);
		if (method == null)
			return fallbackAliasForMethod(stmt, d1, taintedPath);

		wrapperHits++;

		Set<Abstraction> outSet = new AliasAbstractionSet();
		boolean killCurrentTaint = false;
		for (ICollectionOperation op : method.aliasOperations()) {
			killCurrentTaint |= op.apply(d1, taintedPath, stmt, manager, strategy, outSet);
		}

		if (!killCurrentTaint)
			outSet.add(taintedPath);

		return outSet;
	}

	public CollectionMethod getCollectionMethodForSootMethod(SootMethod sm) {
		CollectionModel model = models.get(sm.getDeclaringClass().getName());
		if (model != null) {
			CollectionMethod cm = model.getMethod(sm.getSubSignature());
			if (cm != null)
				return cm;
		}

		List<SootClass> ifcs = new ArrayList<>(sm.getDeclaringClass().getInterfaces());
		SootClass currentClass = sm.getDeclaringClass().getSuperclassUnsafe();
		while (currentClass != null) {
			model = models.get(currentClass.getName());
			if (model != null) {
				CollectionMethod cm = model.getMethod(sm.getSubSignature());
				if (cm != null)
					return cm;
			}
			ifcs.addAll(currentClass.getInterfaces());
			currentClass = currentClass.getSuperclassUnsafe();
		}

		for (SootClass ifc : ifcs) {
			model = models.get(ifc.getName());
			if (model != null) {
				CollectionMethod cm = model.getMethod(sm.getSubSignature());
				if (cm != null)
					return cm;
			}
		}

		return null;
	}

	@Override
	public boolean supportsCallee(SootMethod sm) {
		if (getCollectionMethodForSootMethod(sm) != null)
			return true;

		return fallbackWrapper != null && fallbackWrapper.supportsCallee(sm);
	}

	@Override
	public boolean supportsCallee(Stmt callSite) {
		if (callSite.containsInvokeExpr())
			return supportsCallee(callSite.getInvokeExpr().getMethod());
		return fallbackWrapper != null && fallbackWrapper.supportsCallee(callSite);
	}

	@Override
	public int getWrapperHits() {
		return this.wrapperHits + (fallbackWrapper != null ? fallbackWrapper.getWrapperHits() : 0);
	}

	@Override
	public int getWrapperMisses() {
		return this.wrapperMisses + (fallbackWrapper != null ? fallbackWrapper.getWrapperMisses() : 0);
	}
}
