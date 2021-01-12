package soot.jimple.infoflow.problems.rules.backwardsRules;

import heros.solver.PathEdge;
import soot.*;
import soot.grimp.NewInvokeExpr;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.ByReferenceBoolean;

import java.util.Collection;

public class BackwardsClinitRule extends AbstractTaintPropagationRule {
    public BackwardsClinitRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
        super(manager, zeroValue, results);
    }

    private void propagateToClinit(Abstraction d1, Abstraction abs, Stmt stmt, SootClass declaringClass) {
        Collection<SootMethod> callees = manager.getICFG().getCalleesOfCallAt(stmt);
        // Look through all callees and find the clinit call
        SootMethod callee = callees.stream().filter(c -> c.hasActiveBody() && c.getDeclaringClass() == declaringClass
                && c.getSubSignature().equals("void <clinit>()")).findAny().orElse(null);

        if (callee == null)
            return;

        Collection<Unit> startPoints = manager.getICFG().getStartPointsOf(callee);
        for (Unit startPoint : startPoints)
            manager.getForwardSolver().processEdge(new PathEdge<>(d1, startPoint, abs));
    }

    @Override
    public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        if (!(stmt instanceof AssignStmt))
            return null;
        AssignStmt assignStmt = (AssignStmt) stmt;

        final AccessPath ap = source.getAccessPath();
        final Aliasing aliasing = getAliasing();
        if (aliasing == null)
            return null;

        boolean leftSideMatches = Aliasing.baseMatches(BaseSelector.selectBase(assignStmt.getLeftOp(), false), source);

        Value rightOp = assignStmt.getRightOp();
        Value rightVal = BaseSelector.selectBase(assignStmt.getRightOp(), false);
        if (rightOp instanceof StaticFieldRef && leftSideMatches) {
            SootFieldRef ref = ((StaticFieldRef) rightVal).getFieldRef();
            SootMethod m = manager.getICFG().getMethodOf(stmt);

            // If the static reference is from the same class
            // we will at least find the class NewExpr above.
            if (m.getDeclaringClass() == ref.declaringClass())
                return null;

            // This might be the last occurence of the declaring class of the static reference
            // so we need the visit the clinit method too. This is an overapproximation
            // inherited from the default callgraph algorithm SPARK.
            AccessPath newAp = manager.getAccessPathFactory().copyWithNewValue(ap, rightVal,
                    rightVal.getType(), false);
            Abstraction newAbs = source.deriveNewAbstraction(newAp, stmt);
            if (newAbs != null) {
                newAbs.setCorrespondingCallSite(assignStmt);
                propagateToClinit(d1, newAbs, stmt, ref.declaringClass());
            }
        } else if (rightOp instanceof NewExpr && ap.isStaticFieldRef()) {
            SootClass declaringClass = ((NewExpr) rightOp).getBaseType().getSootClass();
            // If the taint is static and is a field of the instanciated
            // class, this NewExpr might be the last occurence.
            if (ap.getFirstField().getDeclaringClass() == declaringClass)
                propagateToClinit(d1, source, stmt, declaringClass);
        }

        return null;
    }

    @Override
    public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest, ByReferenceBoolean killAll) {
        return null;
    }

    @Override
    public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        return null;
    }

    @Override
    public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt, Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
        // This kills all taints returning from the manual injection of the edge to clinit.
        Stmt callStmt = source.getCorrespondingCallSite();
        if (manager.getICFG().getMethodOf(stmt).getSubSignature().equals("void <clinit>()")
                && callStmt instanceof AssignStmt && ((AssignStmt) callStmt).getRightOp() instanceof StaticFieldRef)
            killAll.value = true;

        return null;
    }
}
