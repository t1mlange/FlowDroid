package soot.jimple.infoflow.problems.rules.backwardsRules;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.util.ByReferenceBoolean;

import java.util.Collection;

public class BackwardsImplicitFlowRule extends AbstractTaintPropagationRule {
    public BackwardsImplicitFlowRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
        super(manager, zeroValue, results);
    }

    @Override
    public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
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
        return null;
    }
}
