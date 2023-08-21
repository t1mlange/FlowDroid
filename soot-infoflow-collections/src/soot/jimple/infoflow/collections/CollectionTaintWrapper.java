package soot.jimple.infoflow.collections;

import java.util.*;
import java.util.stream.Collectors;

import heros.DontSynchronize;
import heros.SynchronizedBy;
import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.analyses.ReadOnlyListViewAnalysis;
import soot.jimple.infoflow.collections.data.CollectionMethod;
import soot.jimple.infoflow.collections.data.CollectionModel;
import soot.jimple.infoflow.collections.operations.ICollectionOperation;
import soot.jimple.infoflow.collections.operations.LocationDependentOperation;
import soot.jimple.infoflow.collections.operations.forward.AbstractShiftOperation;
import soot.jimple.infoflow.collections.operations.forward.ComputeFRPSHandler;
import soot.jimple.infoflow.collections.solver.fastSolver.AppendingCollectionInfoflowSolver;
import soot.jimple.infoflow.collections.solver.fastSolver.CoarserReuseCollectionInfoflowSolver;
import soot.jimple.infoflow.collections.solver.fastSolver.CollectionInfoflowSolver;
import soot.jimple.infoflow.collections.strategies.appending.DefaultAppendingStrategy;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;
import soot.jimple.infoflow.collections.strategies.subsuming.LargerContextSubsumingStrategy;
import soot.jimple.infoflow.collections.strategies.widening.WideningOnRevisitStrategy;
import soot.jimple.infoflow.collections.strategies.widening.WideningStrategy;
import soot.jimple.infoflow.collections.util.AliasAbstractionSet;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

/**
 * Taint Wrapper that models the behavior of collections w.r.t. keys or indices
 *
 * @author Tim Lange
 */
public class CollectionTaintWrapper implements ITaintPropagationWrapper {
	public static ReadOnlyListViewAnalysis itAnalysis;

	@SynchronizedBy("Read-only during analysis")
	private Map<String, CollectionModel> models;

	@SynchronizedBy("Read-only during analysis")
	private final ITaintPropagationWrapper fallbackWrapper;

	@DontSynchronize("Benign race")
	private int wrapperHits;
	@DontSynchronize("Benign race")
	private int wrapperMisses;

	protected InfoflowManager manager;

	private IContainerStrategy strategy;

	private ComputeFRPSHandler frpsHandler;


	private static Type collectionType;
	private static SootClass listClass;
	private static SootClass queueClass;

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

		collectionType = RefType.v("java.util.Collection");
		listClass = Scene.v().getSootClassUnsafe("java.util.List");
		queueClass = Scene.v().getSootClassUnsafe("java.util.Queue");

		for (CollectionModel cm : this.models.values())
			cm.initialize();

		this.strategy = getStrategy();
		this.frpsHandler = new ComputeFRPSHandler(manager);
		manager.getMainSolver().setFollowReturnsPastSeedsHandler(this.frpsHandler);

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
				.map(CollectionMethod::getSignature) // get the subsig
				.collect(Collectors.toUnmodifiableSet());

		Set<SootField> fields = this.models.values().stream()
				.flatMap(model -> model.getAllMethods().stream())
				.flatMap(method -> Arrays.stream(method.operations()))
				.filter(op -> op instanceof LocationDependentOperation)
				.map(op -> (LocationDependentOperation) op)
				.map(LocationDependentOperation::getField)
				.collect(Collectors.toSet());

		IInfoflowSolver solver = manager.getMainSolver();
		if (solver instanceof CollectionInfoflowSolver)
			((CollectionInfoflowSolver) solver).setWideningStrategy(w);
		if (solver instanceof CoarserReuseCollectionInfoflowSolver)
			((CoarserReuseCollectionInfoflowSolver) solver).setSubsuming(new LargerContextSubsumingStrategy(manager));
		if (solver instanceof AppendingCollectionInfoflowSolver)
			((AppendingCollectionInfoflowSolver) solver).setAppendingStrategy(new DefaultAppendingStrategy(manager, fields, allSubSigs));

		solver = manager.getAliasSolver();
		// Because the alias solver does not shift, we do not need to widen
		if (solver instanceof CoarserReuseCollectionInfoflowSolver)
			((CoarserReuseCollectionInfoflowSolver) solver).setSubsuming(new LargerContextSubsumingStrategy(manager));
		if (solver instanceof AppendingCollectionInfoflowSolver)
			((AppendingCollectionInfoflowSolver) solver).setAppendingStrategy(new DefaultAppendingStrategy(manager, fields, allSubSigs));

		itAnalysis = new ReadOnlyListViewAnalysis(manager.getICFG());
	}

	@Override
	public Collection<PreAnalysisHandler> getPreAnalysisHandlers() {
		if (fallbackWrapper != null)
			return fallbackWrapper.getPreAnalysisHandlers();

		return Collections.emptyList();
	}

	protected IContainerStrategy getStrategy() {
		return new TestConstantStrategy(manager, this);
	}

	private Set<Abstraction> fallbackTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
		if (fallbackWrapper != null)
			return fallbackWrapper.getTaintsForMethod(stmt, d1, taintedPath);

		wrapperMisses++;
		return null;
	}

	public void registerCallback(Abstraction d1, Stmt stmt, Abstraction curr, Abstraction queued, SootMethod sm) {
		frpsHandler.registerCallback(d1, stmt, curr, queued, sm);
	}

	@Override
	public Set<Abstraction> getTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
		if (!stmt.containsInvokeExpr())
			return fallbackTaintsForMethod(stmt, d1, taintedPath);

		SootMethod sm = maybeResolveSootMethod(stmt.getInvokeExpr());
		if (sm == null)
			return fallbackAliasForMethod(stmt, d1, taintedPath);
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

		SootMethod sm = maybeResolveSootMethod(stmt.getInvokeExpr());
		if (sm == null)
			return fallbackAliasForMethod(stmt, d1, taintedPath);
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

	/**
	 * Special case: we might see the current call to the Collection class, yet we do not know whether
	 * the collection is a Set, List or Queue. To prevent resolving the size for sets, which we do smash
	 * anyway, we use SPARK to approximate whether this is a set or not.
	 *
	 * @param ie invoke expression
	 * @return the corresponding soot method or null, if not possible
	 */
	private SootMethod maybeResolveSootMethod(InvokeExpr ie) {
		if (!(ie instanceof InstanceInvokeExpr))
			return ie.getMethod();

		Local base = (Local) ((InstanceInvokeExpr) ie).getBase();
		if (!base.getType().equals(collectionType))
			return ie.getMethod();

		FastHierarchy fh = Scene.v().getFastHierarchy();
		Set<Type> types = Scene.v().getPointsToAnalysis().reachingObjects(base).possibleTypes();
		String subsig = ie.getMethod().getSubSignature();
		SootMethod methodCand = types.stream()
				.filter(t -> t instanceof RefType && ((RefType) t).hasSootClass())
				.map(t -> ((RefType) t).getSootClass())
				.filter(sc -> (listClass != null && fh.isSubclass(sc, listClass))
							  || (queueClass != null && fh.isSubclass(sc, queueClass)))
				.map(sc -> sc.getMethodUnsafe(subsig))
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);
		return methodCand;
	}

	public CollectionMethod getCollectionMethodForSootMethod(SootMethod sm) {
		FastHierarchy fh = Scene.v().getFastHierarchy();
		String subsig = null;
		CollectionModel model = models.get(sm.getDeclaringClass().getName());
		if (model != null && model.isNotExcluded(sm.getDeclaringClass(), fh)) {
			subsig = sm.getSubSignature();
			CollectionMethod cm = model.getMethod(subsig);
			if (cm != null)
				return cm;
		}

		List<SootClass> ifcs = new ArrayList<>(sm.getDeclaringClass().getInterfaces());
		SootClass currentClass = sm.getDeclaringClass().getSuperclassUnsafe();
		while (currentClass != null) {
			model = models.get(currentClass.getName());
			if (model != null && model.isNotExcluded(currentClass, fh)) {
				if (subsig == null)
					subsig = sm.getSubSignature();
				CollectionMethod cm = model.getMethod(subsig);
				if (cm != null)
					return cm;
			}
			ifcs.addAll(currentClass.getInterfaces());
			currentClass = currentClass.getSuperclassUnsafe();
		}

		for (SootClass ifc : ifcs) {
			model = models.get(ifc.getName());
			if (model != null && model.isNotExcluded(ifc, fh)) {
				if (subsig == null)
					subsig = sm.getSubSignature();
				CollectionMethod cm = model.getMethod(subsig);
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

	public boolean isLocationDependent(Stmt callSite) {
		return callSite.containsInvokeExpr() && isLocationDependent(callSite.getInvokeExpr().getMethod());
	}

	public boolean isLocationDependent(SootMethod sm) {
		CollectionMethod cm =  getCollectionMethodForSootMethod(sm);
		if (cm == null)
			return false;

		for (ICollectionOperation op : cm.operations())
			if (op instanceof LocationDependentOperation)
				return true;

		return false;
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
