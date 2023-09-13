package soot.jimple.infoflow.collections.strategies.widening;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.PositionBasedContext;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;
import soot.util.ConcurrentHashMultiMap;
import soot.util.IdentityHashSet;

/**
 * Widens each fact that revisits a statement
 *
 * @author Tim Lange
 */
public class WideningOnRevisitStrategy extends AbstractWidening {
	// Cache of abstractions seen at a shift statement
	private final ConcurrentHashMultiMap<Unit, Abstraction> seenAbstractions;

	// Contains all subsignatures that may result in an infinite domain
	private final Set<String> subSigs;

	public WideningOnRevisitStrategy(InfoflowManager manager, Set<String> subSigs) {
		super(manager);
		this.seenAbstractions = new ConcurrentHashMultiMap<>();
		this.subSigs = subSigs;
	}

	private boolean isWideningCandidate(Abstraction abs) {
		if (abs.getAccessPath().getFragmentCount() == 0)
			return false;

		boolean hasPositionContext = false;
		for (AccessPathFragment fragment : abs.getAccessPath().getFragments()) {
			if (fragment.hasContext()) {
				for (ContextDefinition ctxt : fragment.getContext()) {
					if (ctxt instanceof PositionBasedContext) {
						hasPositionContext = true;
						break;
					}
				}
			}
		}

		return hasPositionContext;
	}

	@Override
	public void recordNewFact(Abstraction fact, Unit u) {
		// Only shifting can produce infinite ascending chains
		Stmt stmt = (Stmt) u;
		if (stmt.containsInvokeExpr()
				&& subSigs.contains(stmt.getInvokeExpr().getMethod().getSubSignature())
				&& isWideningCandidate(fact))
			seenAbstractions.put(u, fact);
	}

	@Override
	public Abstraction widen(Abstraction abs, Unit u) {
		// Only context in the domain are infinite
		if (abs.getAccessPath().getFragmentCount() == 0)
			return abs;

		Stmt stmt = (Stmt) u;
		// Only shifting can produce infinite ascending chains
		if (!stmt.containsInvokeExpr()
				|| !subSigs.contains(stmt.getInvokeExpr().getMethod().getSubSignature()))
			return abs;

		IdentityHashSet<Abstraction> visited = new IdentityHashSet<>();
		Deque<Abstraction> q = new ArrayDeque<>();
		q.add(abs);
		while (!q.isEmpty()) {
			Abstraction pred = q.pop();
			if (seenAbstractions.contains(u, pred)) {
				// Widen
				return forceWiden(abs, u);
			}

			if (visited.add(pred.getPredecessor()))
				q.add(pred.getPredecessor());
			abs.getNeighbors().forEach(
					n -> {
						if (visited.add(n))
							q.add(n);
					}
			);
		}

		return abs;
	}
}
