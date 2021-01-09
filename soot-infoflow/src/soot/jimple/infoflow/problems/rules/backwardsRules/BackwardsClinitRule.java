package soot.jimple.infoflow.problems.rules.backwardsRules;

import heros.solver.PathEdge;
import soot.*;
import soot.grimp.NewInvokeExpr;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
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

    @Override
    public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        if (!(stmt instanceof AssignStmt))
            return null;
        AssignStmt assignStmt = (AssignStmt) stmt;

        final AccessPath ap = source.getAccessPath();
        final Aliasing aliasing = getAliasing();
        if (aliasing == null)
            return null;

        // We only need to visit the clinit edge if the rhs is a StaticFieldRef and the lhs is tainted
        boolean leftSideMatches = Aliasing.baseMatches(BaseSelector.selectBase(assignStmt.getLeftOp(), false), source);
        if (ap != null && leftSideMatches) {
            Value val = BaseSelector.selectBase(assignStmt.getRightOp(), false);
            if (val instanceof StaticFieldRef) {
                SootFieldRef ref = ((StaticFieldRef) val).getFieldRef();
                Collection<SootMethod> callees = manager.getICFG().getCalleesOfCallAt(stmt);
                // Look through all callees and find the clinit call
                SootMethod callee = callees.stream().filter(c -> c.hasActiveBody() && c.getDeclaringClass() == ref.declaringClass()
                        && c.getSubSignature().equals("void <clinit>()")).findAny().orElse(null);

                if (callee == null)
                    return null;

                AccessPath newAp = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                        val, val.getType(), true);
                Abstraction newAbs = source.deriveNewAbstraction(newAp, stmt);
                if (newAbs != null) {
                    newAbs.setCorrespondingCallSite(assignStmt);

                    Collection<Unit> startPoints = manager.getICFG().getStartPointsOf(callee);
                    for (Unit startPoint : startPoints)
                        manager.getForwardSolver().processEdge(new PathEdge<>(d1, startPoint, newAbs));
                }
            }
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
