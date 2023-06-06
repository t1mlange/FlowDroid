package soot.jimple.infoflow.collections.operations.forward;

import java.util.Collection;

import soot.Local;
import soot.PointsToSet;
import soot.Scene;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.data.ParamIndex;
import soot.jimple.infoflow.collections.operations.LocationDependentOperation;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;
import soot.jimple.spark.sets.PointsToSetInternal;

public abstract class AbstractShiftOperation extends LocationDependentOperation {

	public AbstractShiftOperation(Location[] keys, String field) {
		super(keys, field);
		assert keys.length == 1; // TODO: generalize
	}

	protected abstract ContextDefinition shift(ContextDefinition ctxt, Stmt stmt, boolean exact,
			IContainerStrategy strategy);

	@Override
	public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager,
			IContainerStrategy strategy, Collection<Abstraction> out) {
		InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
		Local base = (Local) iie.getBase();
		boolean isUnique;
		if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), base)) {
			// We also need to shift if the base and the taint MAY alias. But we need the
			// answer to that now, which clashes with the on-demand alias resolving of
			// FlowDroid. Especially because we are not really able to correlate that a flow
			// originated from an alias query here. So we use some coarser approximations to
			// find out whether we need to shift or not.

			// First, we check whether they must alias, which allows us to strong update
			// here
			boolean mustAlias = manager.getAliasing().mustAlias(incoming.getAccessPath().getPlainValue(), base, stmt);
			if (mustAlias) {
				isUnique = true;
			} else {
				PointsToSet basePts = Scene.v().getPointsToAnalysis().reachingObjects(base);
				PointsToSet incomingPts = Scene.v().getPointsToAnalysis().reachingObjects(base);

				// If even the points-to set says they can't alias, we assume that they don't
				// alias
				if (!basePts.hasNonEmptyIntersection(incomingPts))
					return false;

				// Use the uniqueness property (variable points-to exactly one alloc site) to
				// check whether we can perform a strong update
				isUnique = basePts instanceof PointsToSetInternal && ((PointsToSetInternal) basePts).size() == 1;
			}
		} else {
			isUnique = true;
		}

		AccessPathFragment fragment = incoming.getAccessPath().getFirstFragment();
		// Either when the field doesn't match or there is no index to shift, we can
		// stop here
		if (!fragment.getField().getSignature().equals(this.field) || !fragment.hasContext())
			return false;

		ContextDefinition[] ctxts = fragment.getContext();
		assert ctxts.length == 1;
		ContextDefinition ctxt = ctxts[0];

		int idx = locations[0].getParamIdx();
		Tristate t;
		if (idx == ParamIndex.ALL) {
			t = Tristate.MAYBE();
		} else if (idx >= 0) {
			ContextDefinition stmtCtxt = strategy.getIndexContext(iie.getArg(locations[0].getParamIdx()), stmt);
			t = strategy.lessThanEqual(stmtCtxt, ctxt);
		} else {
			throw new RuntimeException("Unexpected shift index: " + idx);
		}

		// If the insert might be in front of this index, we have to shift
		if (!t.isFalse()) {
			// We can only perform strong updates when the index fully matches and the local
			// definitely matches
			boolean strongUpdate = isUnique && t.isTrue();

			ContextDefinition newCtxt = shift(ctxt, stmt, strongUpdate, strategy);
			AccessPathFragment[] oldFragments = incoming.getAccessPath().getFragments();
			AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length];
			System.arraycopy(oldFragments, 0, fragments, 1, fragments.length - 1);
			if (newCtxt == UnknownContext.v())
				fragments[0] = fragment.copyWithNewContext(null);
			else
				fragments[0] = fragment.copyWithNewContext(new ContextDefinition[] { newCtxt });
			AccessPath ap = manager.getAccessPathFactory().createAccessPath(base, fragments,
					incoming.getAccessPath().getTaintSubFields());
			if (ap != null)
				out.add(incoming.deriveNewAbstraction(ap, stmt));
			return true;
		}

		return false;
	}
}
