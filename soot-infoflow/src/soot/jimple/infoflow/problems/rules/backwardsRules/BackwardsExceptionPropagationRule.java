package soot.jimple.infoflow.problems.rules.backwardsRules;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.util.ByReferenceBoolean;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Rule for propagating exceptional data flows
 *
 * @author Steven Arzt
 *
 */
public class BackwardsExceptionPropagationRule extends AbstractTaintPropagationRule {

	public BackwardsExceptionPropagationRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
	    final Aliasing aliasing = getAliasing();
	    if (aliasing == null)
	        return null;

		// Do we catch an exception here?
		// $stack := @caughtexception
		if (stmt instanceof IdentityStmt) {
			IdentityStmt id = (IdentityStmt) stmt;
			if (id.getRightOp() instanceof CaughtExceptionRef &&
                    aliasing.mayAlias(id.getLeftOp(), source.getAccessPath().getPlainValue())) {
				// Kill the old taint
				killSource.value = true;

				// We leave it to another propagation of normal flow or the call flow function
                // to find the responsible throw stmt
                return Collections.singleton(source.deriveNewAbstractionOnThrow(id));
			}
		}

        // If the exception is from the same method,
        // the next statement is a throw statement
		if (source.getExceptionThrown() && stmt instanceof ThrowStmt) {
			// Kill the old taint
			killSource.value = true;
			// Taint the thrown value
			AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
					((ThrowStmt) stmt).getOp());
			if (ap != null)
				return Collections.singleton(source.deriveNewAbstractionOnCatch(ap));
		}

		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		// if the taint is created from a catch, we just propagate it into the
		// next call but not over it
		if (source.getExceptionThrown())
			killSource.value = true;

		return null;
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt,
			Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
		// If source.getExceptionThrown() is true we know the taint was catched
		// from an thrown exception. Now we need to propagate this taint into
		// the method containing the throw statement
		if (source.getExceptionThrown()) {
            HashSet<Abstraction> res = new HashSet<>();

            // We have to find the throw statement responsible for this taint
			for (Unit unit : dest.getActiveBody().getUnits()) {
				if (unit instanceof ThrowStmt) {
					Value op = ((ThrowStmt) unit).getOp();

					// Only propagate if types match
					if (!manager.getTypeUtils().checkCast(source.getAccessPath(), op.getType()))
						continue;

					AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(), op);
					res.add(source.deriveNewAbstractionOnCatch(ap));
				}
			}

			return res.isEmpty() ? null : res;
		}

		return null;
	}
}
