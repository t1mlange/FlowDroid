package soot.jimple.infoflow.collections.strategies.widening;

import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.PositionBasedContext;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;
import soot.util.ConcurrentHashMultiMap;

import java.util.Set;

/**
 * Widens each fact that revisits a statement
 *
 * @author Tim Lange
 */
public class WideningOnRevisitStrategy implements WideningStrategy<Unit, Abstraction> {
	private final InfoflowManager manager;

	private final ConcurrentHashMultiMap<Unit, Abstraction> seenAbstractions;

	// Contains all subsignatures that may result in an infinite domain
	private final Set<String> subSigs;

	public WideningOnRevisitStrategy(InfoflowManager manager, Set<String> subSigs) {
		this.manager = manager;
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
		if (isWideningCandidate(fact))
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

		AccessPathFragment[] oldFragments = abs.getAccessPath().getFragments();
		AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length];
		System.arraycopy(oldFragments, 1, fragments, 1, fragments.length - 1);
		fragments[0] = oldFragments[0].copyWithNewContext(null);
		AccessPath ap = manager.getAccessPathFactory().createAccessPath(abs.getAccessPath().getPlainValue(), fragments,
				abs.getAccessPath().getTaintSubFields());
		return abs.deriveNewAbstraction(ap, null);
	}
}
