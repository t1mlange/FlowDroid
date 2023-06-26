package soot.jimple.infoflow.collections.strategies.widening;

import java.util.Set;

import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.PositionBasedContext;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;
import soot.util.ConcurrentHashMultiMap;

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

		AccessPathFragment fragment = abs.getAccessPath().getFirstFragment();
		if (!fragment.hasContext())
			return false;

		for (ContextDefinition ctxt : fragment.getContext())
			if (ctxt instanceof PositionBasedContext)
				return true;

		return false;
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
		if (abs.getAccessPath().getFragmentCount() == 0 || !abs.getAccessPath().getFirstFragment().hasContext())
			return abs;

		Stmt stmt = (Stmt) u;
		// Only shifting can produce infinite ascending chains
		if (!stmt.containsInvokeExpr()
				|| !subSigs.contains(stmt.getInvokeExpr().getMethod().getSubSignature()))
			return abs;

		boolean seen = false;
		Abstraction pred = abs;
		while (pred != null) {
			if (seenAbstractions.contains(u, pred)) {
				seen = true;
				break;
			}
			pred = pred.getPredecessor();
		}

		// If we haven't seen the abstraction, we can stop here
		if (!seen)
			return abs;

		return forceWiden(abs, u);
	}
}
