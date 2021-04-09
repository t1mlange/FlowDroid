package soot.jimple.infoflow.problems.rules.backwardsRules;

import heros.solver.PathEdge;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.UnitWithContext;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG.UnitContainer;
import soot.jimple.infoflow.sourcesSinks.manager.IReversibleSourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;
import soot.jimple.infoflow.util.ByReferenceBoolean;

import java.util.*;

/**
 * Implicit flows for the backward direction.
 *
 * @author Tim Lange
 */
public class BackwardsImplicitFlowRule extends AbstractTaintPropagationRule {
    public BackwardsImplicitFlowRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
        super(manager, zeroValue, results);
    }

    @Override
    public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        assert !source.getAccessPath().isEmpty() || source.getDominator() != null;

        if (source == getZeroValue())
            return null;

        // We leave a conditional and taint the condition
        if (source.isDominator(stmt)) {

            // never let empty ap out of conditional
            if (!source.getAccessPath().isEmpty()) {
                killAll.value = true;
                return null;
            }
            killSource.value = true;

            Value condition;
            if (stmt instanceof IfStmt)
                condition = ((IfStmt) stmt).getCondition();
            else if (stmt instanceof SwitchStmt)
                condition = ((SwitchStmt) stmt).getKey();
            else
                return null;

            UnitContainer condUnit = manager.getICFG().getDominatorOf(stmt);
            Set<Abstraction> res = new HashSet<>();
            // We observe the condition at leaving and taint the conditions here.
            if (condition instanceof Local) {
                AccessPath ap = manager.getAccessPathFactory().createAccessPath(condition, false);
                Abstraction abs = source.deriveCondition(ap, stmt);
                res.add(abs);
                if (condUnit.getUnit() != null)
                    res.add(abs.deriveNewAbstractionWithDominator(condUnit.getUnit()));
                return res;
            } else {
                for (ValueBox box : condition.getUseBoxes()) {
                    if (box.getValue() instanceof Constant)
                        continue;

                    AccessPath ap = manager.getAccessPathFactory().createAccessPath(box.getValue(), false);
                    Abstraction abs = source.deriveCondition(ap, stmt);
                    res.add(abs);
                    if (condUnit.getUnit() != null)
                        res.add(abs.deriveNewAbstractionWithDominator(condUnit.getUnit()));
                }
                return res;
            }
        }

        if (manager.getICFG().isExceptionalEdgeBetween(stmt, destStmt) && source.getAccessPath().isEmpty()) {
            if (destStmt instanceof AssignStmt) {
                AccessPath ap = manager.getAccessPathFactory().createAccessPath(((AssignStmt) destStmt).getLeftOp(), false);
                Abstraction abs = source.deriveNewAbstraction(ap, stmt);
                return Collections.singleton(abs);
            }
            return null;
        }

        // Already empty APs stay the same
        if (source.getAccessPath().isEmpty())
            return null;


        UnitContainer dominator = manager.getICFG().getDominatorOf(stmt);
        // When a taint which just has been handed over
        if (!source.getAccessPath().isEmpty() && source.isAbstractionActive()
                && source.getPredecessor() != null && !source.getPredecessor().isAbstractionActive()) {
            // We maybe turned around inside a conditional, so we reconstruct the condition dominator
            // Also, we lost track of the dominators in the alias search. Thus, this is interprocedural.
            // See ImplicitTests#conditionalAliasingTest
            List<Unit> condUnits = manager.getICFG().getConditionalBranchesInterprocedural(stmt);
            // No condition path -> no need to search for one
            for (Unit condUnit : condUnits) {
                Abstraction abs = source.deriveNewAbstractionWithDominator(condUnit);
                if (abs != null)
                    manager.getForwardSolver().processEdge(new PathEdge<>(d1, stmt, abs));
            }
            return null;
//            return Collections.singleton(source.deriveConditionalUpdate(stmt));
        }


        if (source.getExceptionThrown() && stmt instanceof ThrowStmt) {
            Unit condUnit = manager.getICFG().getConditionalBranchIntraprocedural(stmt);
            // No conditional on the intraprocedural path -> no need to search for one
            if (condUnit != null) {
                AccessPath ap = manager.getAccessPathFactory().createAccessPath(((ThrowStmt) stmt).getOp(), false);
                Abstraction absCatch = source.deriveNewAbstractionOnCatch(ap);
                Abstraction abs = absCatch.deriveNewAbstractionWithDominator(condUnit);
                return Collections.singleton(abs);
            }
            return null;
        }

        // Taint enters a conditional branch
        // Only handle cases where the taint is not part of the statement
        // Other cases are in the core flow functions to prevent code duplication
        boolean taintAffectedByStatement = stmt instanceof DefinitionStmt && getAliasing().mayAlias(((DefinitionStmt) stmt).getLeftOp(),
                source.getAccessPath().getPlainValue());
        if (dominator.getUnit() != null && dominator.getUnit() != destStmt && !taintAffectedByStatement) {
            Abstraction abs = source.deriveNewAbstractionWithDominator(dominator.getUnit(), stmt);
            return Collections.singleton(abs);
        }

        return null;
    }

    @Override
    public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest, ByReferenceBoolean killAll) {
        assert !source.getAccessPath().isEmpty() || source.getDominator() != null;

        if (source == getZeroValue())
            return null;

        // Make sure no conditional taint leaves the conditional branch
        if (source.isDominator(stmt)) {
            killAll.value = true;
            return null;
        }

        // We do not propagate empty taints into methods
        // because backward no taints are derived from empty taints.
        if (source.getAccessPath().isEmpty()) {
            killAll.value = true;
            return null;
        }

        return null;
    }

    @Override
    public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        // Every call to a sink inside a conditional is considered a taint
//        if (source == getZeroValue() && manager.getSourceSinkManager() instanceof IReversibleSourceSinkManager) {
//            killSource.value = true;
//            IReversibleSourceSinkManager ssm = (IReversibleSourceSinkManager) manager.getSourceSinkManager();
//
//            SourceInfo sink = ssm.getInverseSinkInfo(stmt, manager);
//            if (sink != null) {
//                HashSet<Abstraction> res = new HashSet<>();
//                SootMethod sm = manager.getICFG().getMethodOf(stmt);
//
//                List<Unit> condUnits = manager.getICFG().getConditionalBranchesInterprocedural(stmt);
//                for (Unit condUnit : condUnits) {
//                    Abstraction abs = new Abstraction(sink.getDefinition(), AccessPath.getEmptyAccessPath(), stmt,
//                            sink.getUserData(), false, false);
//                    abs.setDominator(condUnit);
//                    res.add(abs);
//                }
//
//                if (!sm.isStatic()) {
//                    AccessPath thisAp = manager.getAccessPathFactory().createAccessPath(sm.getActiveBody().getThisLocal(), false);
//                    Abstraction thisTaint = new Abstraction(sink.getDefinition(), thisAp, stmt, sink.getUserData(), false, false);
//                    res.add(thisTaint);
//                }
//
//                return res;
//            }
//        }

        if (source == getZeroValue())
            return null;

        // Kill conditional taints leaving the branch
        if (source.isDominator(stmt)) {
            killAll.value = true;
            return null;
        }

        UnitContainer dominator = manager.getICFG().getDominatorOf(stmt);
        // Taint enters a conditional branch
        // Only handle cases where the taint is not part of the statement
        // Other cases are in the core flow functions to prevent code duplication
        boolean taintAffectedByStatement = stmt instanceof DefinitionStmt && getAliasing().mayAlias(((DefinitionStmt) stmt).getLeftOp(),
                source.getAccessPath().getPlainValue());
        taintAffectedByStatement |= stmt.containsInvokeExpr() && stmt.getInvokeExpr().getArgs().stream()
                .anyMatch(a -> getAliasing().mayAlias(source.getAccessPath().getPlainValue(), a));
        if (dominator.getUnit() != null && !taintAffectedByStatement) {
            Abstraction abs = source.deriveNewAbstractionWithDominator(dominator.getUnit(), stmt);
            return Collections.singleton(abs);
        }

        return null;
    }

    @Override
    public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt, Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
        assert !source.getAccessPath().isEmpty() || source.getDominator() != null;

        if (source == getZeroValue())
            return null;

        if (source.getAccessPath().isEmpty()) {
            // Derived from a conditional taint inside the callee
            // Already has the right dominator
            return Collections.singleton(source.deriveNewAbstraction(source.getAccessPath(), stmt));
        }

        SootMethod callee = manager.getICFG().getMethodOf(stmt);
        List<Local> params = callee.getActiveBody().getParameterLocals();
        InvokeExpr ie = callSite.containsInvokeExpr() ? callSite.getInvokeExpr() : null;
        // In the callee, a parameter influenced a sink. If the argument was an constant
        // we need another implicit taint
//        if (ie != null) {
//            for (int i = 0; i < params.size() && i < ie.getArgCount(); i++) {
//                if (getAliasing().mayAlias(source.getAccessPath().getPlainValue(), params.get(i))
//                        && ie.getArg(i) instanceof Constant) {
//                    HashSet<Abstraction> res = new HashSet<>();
//                    Unit condUnit = manager.getICFG().getConditionalBranchIntraprocedural(callSite);
//                    if (condUnit != null) {
//                        Abstraction intraRet = source.deriveNewAbstractionWithDominator(condUnit, stmt);
//                        intraRet.setCorrespondingCallSite(callSite);
//                        return Collections.singleton(intraRet);
//                    }
//                }
//            }
//        }

        return null;
    }
}
