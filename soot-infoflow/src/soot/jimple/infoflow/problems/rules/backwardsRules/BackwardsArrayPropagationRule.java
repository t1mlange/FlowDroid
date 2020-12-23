package soot.jimple.infoflow.problems.rules.backwardsRules;

import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPath.ArrayTaintType;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.TypeUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Rule for propagating array accesses
 * 
 * @author Steven Arzt
 *
 */
public class BackwardsArrayPropagationRule extends AbstractTaintPropagationRule {

	public BackwardsArrayPropagationRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		// Get the assignment
		if (!(stmt instanceof AssignStmt))
			return null;
		AssignStmt assignStmt = (AssignStmt) stmt;

		Abstraction newAbs = null;
		final Value leftVal = assignStmt.getLeftOp();
		final Value rightVal = assignStmt.getRightOp();

		// x = a.length -> a length tainted
		if (rightVal instanceof LengthExpr) {
			LengthExpr lengthExpr = (LengthExpr) rightVal;
			if (leftVal == source.getAccessPath().getPlainValue()) {
				// Is the length tainted? If only the contents are tainted,
				// the incoming abstraction does not match
				if (source.getAccessPath().getArrayTaintType() == ArrayTaintType.Contents)
					return null;

				// Taint the array length
				AccessPath ap = getManager().getAccessPathFactory().createAccessPath(lengthExpr.getOp(), lengthExpr.getOp().getType(), true, ArrayTaintType.Length);
				newAbs = source.deriveNewAbstraction(ap, assignStmt);
			}
		}
		// y = x[i] && y tainted -> x, y tainted
		else if (rightVal instanceof ArrayRef) {
			Value rightBase = ((ArrayRef) rightVal).getBase();
			Value rightIndex = ((ArrayRef) rightVal).getIndex();
			// y = x[i]
			if (source.getAccessPath().getArrayTaintType() != ArrayTaintType.Length
					&& leftVal == source.getAccessPath().getPlainValue()) {
				AccessPath ap;
				// track index
				if (getManager().getConfig().getImplicitFlowMode().trackArrayAccesses()) {
					ap = getManager().getAccessPathFactory().copyWithNewValue(source.getAccessPath(), rightIndex,
							null, false, true, ArrayTaintType.Contents);
				}
				// taint whole array
				else {
					// We add one layer
					Type baseType = source.getAccessPath().getBaseType();
					Type targetType = TypeUtils.buildArrayOrAddDimension(baseType, baseType.getArrayType());

					// Create the new taint abstraction
					ap = getManager().getAccessPathFactory().copyWithNewValue(source.getAccessPath(), rightBase,
							targetType, false, true, ArrayTaintType.Contents);
				}
				newAbs = source.deriveNewAbstraction(ap, assignStmt);
			}
		}
		// y = new A[i], y +length tainted
		else if (rightVal instanceof NewArrayExpr && getManager().getConfig().getEnableArraySizeTainting()) {
			NewArrayExpr newArrayExpr = (NewArrayExpr) rightVal;
			if (!(newArrayExpr.getSize() instanceof Constant) && source.getAccessPath().getArrayTaintType() != ArrayTaintType.Contents
					&& source.getAccessPath().getPlainValue() == leftVal) {
				// Create the new taint abstraction
				AccessPath ap = getManager().getAccessPathFactory().copyWithNewValue(source.getAccessPath(), newArrayExpr.getSize(),
						null, false, true, ArrayTaintType.Length);
				newAbs = source.deriveNewAbstraction(ap, assignStmt);
			}
		}

		if (newAbs == null)
			return null;

		killSource.value = true;
		Set<Abstraction> res = new HashSet<>();
		res.add(newAbs);

		return res;
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt,
			Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		return null;
	}

}
