package soot.jimple.infoflow.problems.rules.backwardsRules;

import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.sourcesSinks.manager.IReversibleSourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.taintWrappers.IReversibleTaintWrapper;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.TypeUtils;
import soot.jimple.infoflow.util.preanalyses.SingleLiveVariableAnalysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class BackwardsWrapperRule extends AbstractTaintPropagationRule {
    public static boolean DEBUG_TW = false;

    public BackwardsWrapperRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
        super(manager, zeroValue, results);
    }

    @Override
    public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        return null;
    }

    @Override
    public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest, ByReferenceBoolean killAll) {
        final ITaintPropagationWrapper wrapper = manager.getTaintWrapper();

        // Can we use the taintWrapper results?
        // If yes, this is done in CallToReturnFlowFunction
        if (wrapper != null && wrapper.isExclusive(stmt, source))
            killAll.value = true;
        return null;
    }

    @Override
    public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        if (source == zeroValue)
            return null;

        if (manager.getTaintWrapper() == null || !(manager.getTaintWrapper() instanceof IReversibleTaintWrapper))
            return null;
        final IReversibleTaintWrapper wrapper = (IReversibleTaintWrapper) manager.getTaintWrapper();

        final Aliasing aliasing = getAliasing();
        if (aliasing == null)
            return null;

        final AccessPath sourceAp = source.getAccessPath();
        boolean isTainted = false;
        boolean retValTainted = false;
        Value leftOp = null;
        if (!sourceAp.isStaticFieldRef() && !sourceAp.isEmpty()) {
            InvokeExpr invokeExpr = stmt.getInvokeExpr();

            // is the return value tainted
            if (stmt instanceof AssignStmt) {
                leftOp = ((AssignStmt) stmt).getLeftOp();
                isTainted = aliasing.mayAlias(leftOp, sourceAp.getPlainValue());
                killSource.value = isTainted;
                retValTainted = isTainted;
            }

            // is the base object tainted
            if (!isTainted && invokeExpr instanceof InstanceInvokeExpr)
                isTainted = aliasing.mayAlias(((InstanceInvokeExpr) invokeExpr).getBase(), sourceAp.getPlainValue());


            // is at least one parameter tainted?
            // we need this because of one special case in EasyTaintWrapper:
            // String.getChars(int srcBegin, int srcEnd, char[] dest, int destBegin)
            // if String is tainted, the third parameter contains the exploded string
            if (!isTainted) {
                if (wrapper instanceof EasyTaintWrapper && invokeExpr.getArgCount() >= 3)
                    isTainted = aliasing.mayAlias(invokeExpr.getArg(2), sourceAp.getPlainValue());
                else
                    isTainted = invokeExpr.getArgs().stream()
                            .anyMatch(arg -> !(arg.getType() instanceof PrimType || TypeUtils.isStringType(arg.getType()))
                                    && aliasing.mayAlias(arg, sourceAp.getPlainValue()));
            }
        }

        if (!isTainted)
            return null;

        if (!getManager().getConfig().getInspectSources() && manager.getSourceSinkManager() != null
                && manager.getSourceSinkManager() instanceof IReversibleSourceSinkManager) {
            final SinkInfo sourceInfo = ((IReversibleSourceSinkManager) manager.getSourceSinkManager())
                    .getInverseSourceInfo(stmt, manager, null);

            if (sourceInfo != null)
                return null;
        }

        Set<Abstraction> res = wrapper.getInverseTaintsForMethod(stmt, d1, source);
        if (res != null) {
            Set<Abstraction> resWAliases = new HashSet<>(res);

            SootMethod sm = manager.getICFG().getMethodOf(stmt);
            boolean intraTurnUnit = source.getTurnUnit() != null && manager.getICFG().getMethodOf(source.getTurnUnit()) == sm;
            for (Abstraction abs : res) {
                AccessPath absAp = abs.getAccessPath();

                // Tainted return values are only sent to alias analysis but never upward
                if (leftOp != null && aliasing.mayAlias(leftOp, absAp.getPlainValue())) {
                    Value rightOp = ((AssignStmt) stmt).getRightOp();
                    boolean localNotReused = rightOp.getUseBoxes().stream()
                            .noneMatch(box -> aliasing.mayAlias(box.getValue(), absAp.getPlainValue()));
                    if (localNotReused)
                        resWAliases.remove(abs);
                }

                // Primitive Assignment -> set turn unit
                if (retValTainted && leftOp != null) {
                    Type t;
                    if (leftOp instanceof FieldRef)
                        t = ((FieldRef) leftOp).getField().getType();
                    else
                        t = leftOp.getType();
                    boolean setTurnUnit = t instanceof PrimType || (TypeUtils.isStringType(t) && !absAp.getCanHaveImmutableAliases());
                    if (setTurnUnit) {
                        abs.setTurnUnit(stmt);
                        continue;
                    }
                }

                // no need to search for aliases if the access path didn't change
                if (!absAp.equals(sourceAp) && !absAp.isEmpty() && !(retValTainted && intraTurnUnit && canOmitAliasing(abs, stmt, sm))) {
                    boolean isBasicString = TypeUtils.isStringType(absAp.getBaseType()) && !absAp.getCanHaveImmutableAliases()
                            && !getAliasing().isStringConstructorCall(stmt);
                    boolean taintsObjectValue = absAp.getBaseType() instanceof RefType && !isBasicString
                            && (absAp.getFieldCount() > 0 || absAp.getTaintSubFields());
                    boolean taintsStaticField = getManager().getConfig()
                            .getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
                            && abs.getAccessPath().isStaticFieldRef()
                            && !(absAp.getFirstFieldType() instanceof PrimType)
                            && !(TypeUtils.isStringType(absAp.getFirstFieldType()));

                    if (taintsObjectValue || taintsStaticField
                            || aliasing.canHaveAliasesRightSide(stmt, abs.getAccessPath().getPlainValue(), abs)) {
                        for (Unit pred : manager.getICFG().getPredsOf(stmt))
                            aliasing.computeAliases(d1, (Stmt) pred, absAp.getPlainValue(), resWAliases,
                                    getManager().getICFG().getMethodOf(pred), abs);
                    } else {
                        abs.setTurnUnit(stmt);
                    }
                }

                if (!killSource.value && absAp.equals(sourceAp))
                    killSource.value = source != abs;
            }
            res = resWAliases;
        }

        if (res != null)
            for (Abstraction abs : res)
                if (abs != source)
                    abs.setCorrespondingCallSite(stmt);

        if (DEBUG_TW)
            System.out.println("In: " + source.toString() + "\n" + "Stmt:" + stmt.toString() + "\n" + (res == null ? "[]" : res.toString()) + "\n");

        return res;
    }

    private static final Set<String> excludeList = parseExcludeList();
    private static Set<String> parseExcludeList() {
        try {
            return Files.lines(Paths.get("BackwardsWrapperExcludeList.txt")).filter(p -> !p.startsWith("#")).collect(Collectors.toSet());
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    /**
     * Optimization for StringBuilders/StringBuffers. Both return this for easy chaining. But Jimple does
     * not reuse the local sometimes. Thus, if at certain methods the local is not reused, we first do an
     * easier check whether an alias search is actually needed to save some edges to maintain our advantage
     * for strict right-to-left ordered assignments.
     * @param abs Abstraction
     * @param callStmt Statement
     * @return True if the aliasing search can be omitted
     */
    private boolean canOmitAliasing(Abstraction abs, Stmt callStmt, SootMethod sm) {
        if (!(callStmt instanceof AssignStmt))
            return false;
        AssignStmt assignStmt = (AssignStmt) callStmt;

        if (!(assignStmt.getRightOp() instanceof InstanceInvokeExpr))
            return false;
        InstanceInvokeExpr ie = (InstanceInvokeExpr) assignStmt.getRightOp();

        if (ie.getBase() == assignStmt.getLeftOp())
            return false;
        if (!getAliasing().mayAlias(ie.getBase(), abs.getAccessPath().getPlainValue()))
            return false;

        if (!excludeList.contains(ie.getMethod().getSignature()))
            return false;

        SingleLiveVariableAnalysis slva = new SingleLiveVariableAnalysis(manager.getICFG().getOrCreateUnitGraph(sm), (Local) ie.getBase(), abs.getTurnUnit());
        return slva.canOmitAlias(callStmt);
    }

    @Override
    public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction calleeD1, Abstraction source, Stmt stmt, Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
        return null;
    }
}
