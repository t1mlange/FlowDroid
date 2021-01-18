package soot.jimple.infoflow.problems.rules.backwardsRules;

import java.util.Collection;

import soot.Local;
import soot.SootMethod;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Propagation rule that implements strong updates
 *
 * @author Steven Arzt
 *
 */
public class BackwardsStrongUpdatePropagationRule extends AbstractTaintPropagationRule {

	public BackwardsStrongUpdatePropagationRule(InfoflowManager manager, Abstraction zeroValue,
									   TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
													   ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		if (!(stmt instanceof AssignStmt))
			return null;
		AssignStmt assignStmt = (AssignStmt) stmt;

		// if leftvalue contains the tainted value -> it is overwritten - remove taint:
		// but not for arrayRefs:
		// x[i] = y --> taint is preserved since we do not distinguish between elements
		// of collections
		// because we do not use a MUST-Alias analysis, we cannot delete aliases of
		// taints
		if (assignStmt.getLeftOp() instanceof ArrayRef)
			return null;

		if (!(assignStmt.getRightOp() instanceof Constant))
			return null;

		// If this is a newly created alias at this statement, we don't kill it right
		// away
		if (source.getCurrentStmt() == stmt)
			return null;

		// If the statement has just been activated, we do not overwrite stuff
//		if (source.getPredecessor() != null && source.getPredecessor().isAbstractionActive()
//				&& source.isAbstractionActive() && source.getAccessPath().equals(source.getPredecessor().getAccessPath()))
//			return null;

		if (source.getAccessPath().isInstanceFieldRef()) {
			// Data Propagation: x.f = y && x.f tainted --> no taint propagated
			// Alias Propagation: Only kill the alias if we directly overwrite it,
			// otherwise it might just be the creation of yet another alias
			if (assignStmt.getLeftOp() instanceof InstanceFieldRef) {
				InstanceFieldRef leftRef = (InstanceFieldRef) assignStmt.getLeftOp();
				if (getAliasing().mustAlias((Local) leftRef.getBase(),
						source.getAccessPath().getPlainValue(), assignStmt)) {
					if (getAliasing().mustAlias(leftRef.getField(), source.getAccessPath().getFirstField())) {
						killAll.value = true;
						return null;
					}
				}
			}
			// x = y && x.f tainted -> no taint propagated. This must only check the precise
			// variable which gets replaced, but not any potential strong aliases
			else if (assignStmt.getLeftOp() instanceof Local) {
				if (getAliasing().mustAlias((Local) assignStmt.getLeftOp(), source.getAccessPath().getPlainValue(),
						stmt)) {
					killAll.value = true;
					return null;
				}
			}
		}
		// X.f = y && X.f tainted -> no taint propagated. Kills are allowed even if
		// static field tracking is disabled
//		else if (source.getAccessPath().isStaticFieldRef()) {
//			if (assignStmt.getLeftOp() instanceof StaticFieldRef && getAliasing().mustAlias(
//					((StaticFieldRef) assignStmt.getLeftOp()).getField(), source.getAccessPath().getFirstField())) {
//				killAll.value = true;
//				return null;
//			}
//
//		}
//		// when the fields of an object are tainted, but the base object is overwritten
//		// then the fields should not be tainted any more
//		// x = y && x.f tainted -> no taint propagated
//		else if (source.getAccessPath().isLocal() && assignStmt.getLeftOp() instanceof Local
//				&& assignStmt.getLeftOp() == source.getAccessPath().getPlainValue()) {
//			// If there is also a reference to the tainted value on the right side, we
//			// must only kill the source, but give the other rules the possibility to
//			// re-create the taint
//			boolean found = false;
//			for (ValueBox vb : assignStmt.getRightOp().getUseBoxes())
//				if (vb.getValue() == source.getAccessPath().getPlainValue()) {
//					found = true;
//					break;
//				}
//
//			killAll.value = !found;
//			killSource.value = true;
//			return null;
//		}

		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
													 ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
															 ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		// Do not propagate abstractions for locals that get overwritten
//		if (stmt instanceof AssignStmt) {
//			AccessPath ap = source.getAccessPath();
//			if (ap != null) {
//				// a = foo() with a tainted
//				AssignStmt assignStmt = (AssignStmt) stmt;
//				final Aliasing aliasing = getAliasing();
//				if (aliasing != null && !ap.isStaticFieldRef() && assignStmt.getLeftOp() instanceof Local
//						&& aliasing.mayAlias(assignStmt.getLeftOp(), ap.getPlainValue()))
//					killSource.value = true;
//			}
//		}

		return null;
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt,
													   Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		return null;
	}

}
