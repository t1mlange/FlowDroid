package soot.jimple.infoflow.solver.gcSolver;

import heros.solver.PathEdge;
import soot.SootMethod;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.util.ConcurrentHashMultiMap;

/**
 * Abstract base class for garbage collectors
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractGarbageCollector<N, D, A> implements IGarbageCollector<N, D> {

	protected BiDiInterproceduralCFG<N, SootMethod> icfg;
	protected ConcurrentHashMultiMap<A, PathEdge<N, D>> jumpFunctions;

	protected final IGCReferenceProvider<A> referenceProvider;

	public AbstractGarbageCollector(IGCReferenceProvider<A> referenceProvider) {
		this.referenceProvider = referenceProvider;
	}

	public AbstractGarbageCollector() {
		this.referenceProvider = createReferenceProvider();
	}

	/**
	 * Initializes the garbage collector
	 */
	public void initialize(BiDiInterproceduralCFG<N, SootMethod> icfg,
							  ConcurrentHashMultiMap<A, PathEdge<N, D>> jumpFunctions) {
		this.icfg = icfg;
		this.jumpFunctions = jumpFunctions;
	}

	/**
	 * Creates the reference provider that garbage collectors can use to identify
	 * dependencies
	 * 
	 * @return The new reference provider
	 */
	protected abstract IGCReferenceProvider<A> createReferenceProvider();

	protected long getRemainingPathEdgeCount() {
		return jumpFunctions.values().size();
	}

}
