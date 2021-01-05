package soot.jimple.infoflow.problems.rules.backwardsRules;

import heros.solver.PathEdge;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
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

        if (ap != null) {
            Value val = BaseSelector.selectBase(assignStmt.getRightOp(), false);
            if (val instanceof StaticFieldRef) {
                SootFieldRef ref = ((StaticFieldRef) val).getFieldRef();
                for (SootMethod caller : manager.getICFG().getCalleesOfCallAt(stmt)) {
                    if (caller.hasActiveBody() && caller.getDeclaringClass() == ref.declaringClass()
                            && caller.getSubSignature().equals("void <clinit>()")) {
                        Unit last = caller.getActiveBody().getUnits().getLast();
                        AccessPath newAp = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                val, val.getType(), false);
                        Abstraction newAbs = source.deriveNewAbstraction(newAp, stmt);
                        if (newAbs != null)
                            manager.getForwardSolver().processEdge(new PathEdge<>(d1, last, newAbs));
                    }
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
        // Do not return taints from clinit methods as the call edge was introduced manually
        // to find sources inside clinit.
        if (manager.getICFG().getMethodOf(stmt).getSubSignature().equals("void <clinit>()")
                && source.getAccessPath().isStaticFieldRef())
            killAll.value = true;

        return null;
    }
}
