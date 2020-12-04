package soot.jimple.infoflow.problems.rules;

import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPath.ArrayTaintType;
import soot.jimple.infoflow.problems.TaintPropagationResults;
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
public class BackwardArrayPropagationRule extends AbstractTaintPropagationRule {

	public BackwardArrayPropagationRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
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

		if (rightVal instanceof LengthExpr) {
			LengthExpr lengthExpr = (LengthExpr) rightVal;
			if (source.getAccessPath().getPlainValue() == leftVal) {
				// Is the length tainted? If only the contents are tainted,
				// the incoming abstraction does not match
				if (source.getAccessPath().getArrayTaintType() == ArrayTaintType.Contents)
					return null;

				// Taint the array length
				AccessPath ap = getManager().getAccessPathFactory().createAccessPath(lengthExpr.getOp(), null, IntType.v(),
						(Type[]) null, true, false, true, ArrayTaintType.ContentsAndLength);
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
					ArrayTaintType arrayTaintType = ArrayTaintType.ContentsAndLength;
					ap = getManager().getAccessPathFactory().copyWithNewValue(source.getAccessPath(), rightIndex,
							null, false, true, arrayTaintType);
				}
				// taint whole array
				else {
					// We add one layer
					Type baseType = source.getAccessPath().getBaseType();
					Type targetType = TypeUtils.buildArrayOrAddDimension(baseType, baseType.getArrayType());

					// Create the new taint abstraction
					ArrayTaintType arrayTaintType = source.getAccessPath().getArrayTaintType();
					ap = getManager().getAccessPathFactory().copyWithNewValue(source.getAccessPath(), rightBase,
							targetType, false, true, arrayTaintType);
				}
				newAbs = source.deriveNewAbstraction(ap, assignStmt);
			}
		}
		// y = new A[i] with i tainted
		else if (rightVal instanceof NewArrayExpr && getManager().getConfig().getEnableArraySizeTainting()) {
			NewArrayExpr newArrayExpr = (NewArrayExpr) rightVal;
			if (!(newArrayExpr.getSize() instanceof Constant) && source.getAccessPath().getPlainValue() == leftVal) {
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
