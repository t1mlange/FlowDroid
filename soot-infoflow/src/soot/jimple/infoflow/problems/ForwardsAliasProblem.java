package soot.jimple.infoflow.problems;

import com.sun.xml.bind.v2.runtime.reflect.opt.Const;
import fj.Hash;
import fj.P;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.flowfunc.Identity;
import heros.flowfunc.KillAll;
import heros.solver.PathEdge;
import polyglot.ast.Assert;
import polyglot.ast.NewArray;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.collect.MutableTwoElementSet;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;
import soot.jimple.infoflow.problems.rules.PropagationRuleManager;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;
import soot.jimple.infoflow.solver.functions.SolverCallFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverCallToReturnFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverNormalFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverReturnFlowFunction;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.infoflow.util.TypeUtils;

import java.util.*;

/**
 * Class which contains the alias analysis for the backwards analysis.
 *
 * @author Tim Lange
 */
public class ForwardsAliasProblem extends AbstractInfoflowProblem {
    private final static boolean DEBUG_PRINT = false;

    public ForwardsAliasProblem(InfoflowManager manager) {
        super(manager);
    }

    @Override
    protected FlowFunctions<Unit, Abstraction, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Unit, Abstraction, SootMethod>() {
            private Abstraction checkAbstraction(Abstraction abs) {
                if (abs == null)
                    return null;

                // Primitive types and strings cannot have aliases and thus
                // never need to be propagated back
                if (!abs.getAccessPath().isStaticFieldRef()) {
                    if (abs.getAccessPath().getBaseType() instanceof PrimType)
                        return null;
                } else {
                    if (abs.getAccessPath().getFirstFieldType() instanceof PrimType)
                        return null;
                }
                return abs;
            }

            @Override
            public FlowFunction<Abstraction> getNormalFlowFunction(Unit srcUnit, Unit destUnit) {
                if (!(srcUnit instanceof DefinitionStmt))
                    return Identity.v();

                final DefinitionStmt defStmt = (DefinitionStmt) srcUnit;
                final DefinitionStmt destDefStmt = destUnit instanceof DefinitionStmt ? (DefinitionStmt) destUnit : null;
                final Value destLeftValue = destDefStmt == null ? null
                        : BaseSelector.selectBase(destDefStmt.getLeftOp(), true);

                return new SolverNormalFlowFunction() {
                    @Override
                    public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
                        if (source == getZeroValue())
                            return null;

                        if (taintPropagationHandler != null)
                            taintPropagationHandler.notifyFlowIn(srcUnit, source, manager,
                                    TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);

                        // TurnUnit is the sink. Below this stmt, the taint is not valid anymore
                        // Therefore we turn around here.
                        if (source.getTurnUnit() == srcUnit) {
                            for (Unit u : interproceduralCFG().getPredsOf(srcUnit))
                                manager.getForwardSolver()
                                        .processEdge(new PathEdge<Unit, Abstraction>(d1, u, source));


                            return notifyOutFlowHandlers(srcUnit, d1, source, null,
                                    TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);
                        }


                        Set<Abstraction> res = computeAliases(defStmt, d1, source);
                        if (DEBUG_PRINT)
                            System.out.println("Alias Normal" + "\n" + "In: " + source.toString() + "\n" + "Stmt: " + srcUnit.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n" + "---------------------------------------");

//                        if (destDefStmt != null && res != null && !res.isEmpty()
//                                && manager.getICFG().isExitStmt(destDefStmt)) {
//                            for (Abstraction abs : res)
//                                computeAliases(destDefStmt, d1, abs);
//                        }

                        return notifyOutFlowHandlers(srcUnit, d1, source, res,
                                TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);
                    }

                    private Set<Abstraction> computeAliases(final DefinitionStmt defStmt, Abstraction d1,
                                                            Abstraction source) {
                        if (defStmt instanceof IdentityStmt)
                            return Collections.singleton(source);
                        if (!(defStmt instanceof AssignStmt))
                            return null;

                        MutableTwoElementSet<Abstraction> res = new MutableTwoElementSet<>();
                        final AssignStmt assignStmt = (AssignStmt) defStmt;
                        final Value leftOp = assignStmt.getLeftOp();
                        final Value rightOp = assignStmt.getRightOp();
                        final Value leftVal = BaseSelector.selectBase(rightOp, false);
                        final Value rightVal = BaseSelector.selectBase(rightOp, false);

                        boolean leftSideMatches = Aliasing.baseMatches(leftOp, source);
                        if (!leftSideMatches) {
                            // Taint is not on the left side, so it needs to be kept
                            res.add(source);
                        } else {
                            // The source taint will end here, so we give it back
                            // to our dat flow solver
                            for (Unit u : manager.getICFG().getPredsOf(assignStmt))
                                manager.getForwardSolver()
                                        .processEdge(new PathEdge<Unit, Abstraction>(d1, u, source));
                        }

                        // BinopExr & UnopExpr operands can not have aliases
                        // as both only can have primitives on the right side
                        // NewArrayExpr do not produce new aliases
                        if (rightOp instanceof BinopExpr || rightOp instanceof UnopExpr || rightOp instanceof NewArrayExpr)
                            return res;


                        // If left side is tainted but right side can not have aliases anyways,
                        // we can stop now as left is overwritten and right is not tracked.
                        if (leftSideMatches && !(rightVal instanceof Local || rightVal instanceof FieldRef))
                            return null;


                        AccessPath ap = source.getAccessPath();
                        Value sourceBase = ap.getPlainValue();
                        boolean addRightValue = false;
                        if ((rightVal instanceof Local || rightVal instanceof FieldRef)) {
                            boolean cutFirstFieldRight = false;
                            Type rightType = null;

                            // S.x
                            if (leftOp instanceof StaticFieldRef) {
                                // If we do not track statics, just skip this rightVal
                                if (getManager().getConfig().getStaticFieldTrackingMode()
                                        != InfoflowConfiguration.StaticFieldTrackingMode.None
                                        && ap.firstFieldMatches(((StaticFieldRef) leftOp).getField())) {
                                    addRightValue = true;
                                    cutFirstFieldRight = true;
                                    rightType = ap.getFirstFieldType();
                                }
                            }
                            // o.x
                            else if (leftOp instanceof InstanceFieldRef) {
                                InstanceFieldRef instRef = ((InstanceFieldRef) leftOp);

                                if (ap.isInstanceFieldRef() && instRef.getBase() == sourceBase
                                        && ap.firstFieldMatches(instRef.getField())) {
                                    addRightValue = true;
                                    // Without o1.x = o2.x would result in o2.x.x
                                    cutFirstFieldRight = true;
                                    rightType = ap.getFirstFieldType();
                                }
                            } else if (leftOp instanceof ArrayRef) {
                                // If we don't track arrays or just the length is tainted we have nothing to do.
                                if (getManager().getConfig().getEnableArrayTracking()
                                        && ap.getArrayTaintType() != AccessPath.ArrayTaintType.Length) {
                                    ArrayRef arrayRef = (ArrayRef) leftOp;
                                    if (arrayRef.getBase() == sourceBase) {
                                        addRightValue = true;
                                        rightType = ((ArrayType) arrayRef.getBase().getType()).getElementType();
                                        if (TypeUtils.isObjectLikeType(rightType))
                                            rightType = rightVal.getType();

                                        if (!manager.getTypeUtils().checkCast(rightVal.getType(), rightType))
                                            addRightValue = false;
                                    }
                                }
                            }
                            // default case
                            else if (leftOp == sourceBase) {
//                                if (!manager.getTypeUtils().checkCast(source.getAccessPath(), rightVal.getType()))
//                                    return null;

                                // CastExpr only make the types more imprecise backwards
                                // InstanceOfExpr are booleans aka primtypes on the left, should not occur
                                // LengthExpr are ints aka primtypes on the left, can not occur
                                // as it is a subclass of UnopExpr

                                addRightValue = true;
//                                rightType = source.getAccessPath().getBaseType();
                            }


                            if (addRightValue) {
                                AccessPath newAp = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                        rightVal, rightType, cutFirstFieldRight);
                                Abstraction newAbs = source.deriveNewAbstraction(newAp, assignStmt);

                                if (newAbs != null && !newAp.equals(ap)) {
                                    if (rightVal instanceof StaticFieldRef && manager.getConfig().getStaticFieldTrackingMode()
                                            == InfoflowConfiguration.StaticFieldTrackingMode.ContextFlowInsensitive)
                                        manager.getGlobalTaintManager().addToGlobalTaintState(newAbs);
                                    else {
                                        res.add(newAbs);

                                        // Inject the new alias into the forward solver
                                        for (Unit u : manager.getICFG().getPredsOf(defStmt))
                                            manager.getForwardSolver()
                                                    .processEdge(new PathEdge<Unit, Abstraction>(d1, u, newAbs));
                                    }
                                }
                            }
                        }

                        if (!(rightVal.getType() instanceof PrimType)
                                && (leftOp instanceof Local || leftOp instanceof FieldRef)) {
                            boolean addLeftValue = false;
                            boolean cutFirstFieldLeft = false;
                            Type leftType = null;

                            if (rightVal instanceof StaticFieldRef) {
                                if (manager.getConfig().getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
                                        && ap.firstFieldMatches(((StaticFieldRef) rightVal).getField())) {
                                    addLeftValue = true;
//                                    leftType = source.getAccessPath().getBaseType();
                                }
                            } else if (rightVal instanceof InstanceFieldRef) {
                                InstanceFieldRef instRef = (InstanceFieldRef) rightVal;

                                if (ap.isInstanceFieldRef() && instRef.getBase() == sourceBase
                                        && ap.firstFieldMatches(instRef.getField())) {
                                    addLeftValue = true;
                                    cutFirstFieldLeft = true;
//                                    leftType = ap.getFirstFieldType();
                                }
                            } else if (rightVal == sourceBase) {
                                addLeftValue = true;
                                leftType = ap.getBaseType();
                                // We did not keep the ArrayRef, so rightVal is already the array base
                                if (rightOp instanceof ArrayRef) {
                                    leftType = ((ArrayType) leftType).getElementType();
                                } else if (leftOp instanceof ArrayRef) {
                                    ArrayRef arrayRef = (ArrayRef) leftOp;
                                    leftType = TypeUtils.buildArrayOrAddDimension(leftType, arrayRef.getType().getArrayType());
                                } else {
                                    if (!manager.getTypeUtils().checkCast(source.getAccessPath(), leftOp.getType()))
                                        return null;
                                }

                                // LengthExpr extends UnopExpr, not possible here
                                if (rightVal instanceof CastExpr) {
                                    CastExpr ce = (CastExpr) rightOp;
                                    if (!manager.getHierarchy().canStoreType(leftType, ce.getCastType()))
                                        leftType = ce.getCastType();
                                } else if (rightVal instanceof InstanceOfExpr) {
                                    // We could just produce a boolean, which won't be tracked anyways
                                    addLeftValue = false;
                                }
                            }

                            if (addLeftValue) {
                                AccessPath newAp = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                        leftOp, leftType, cutFirstFieldLeft);
                                Abstraction newAbs = checkAbstraction(source.deriveNewAbstraction(newAp, assignStmt));
                                if (newAbs != null && newAbs != source) {
                                    if (rightVal instanceof StaticFieldRef && manager.getConfig().getStaticFieldTrackingMode()
                                            == InfoflowConfiguration.StaticFieldTrackingMode.ContextFlowInsensitive)
                                        manager.getGlobalTaintManager().addToGlobalTaintState(newAbs);
                                    else {
                                        res.add(newAbs);

                                        // Inject the new alias into the forward solver
                                        for (Unit u : manager.getICFG().getPredsOf(defStmt))
                                            manager.getForwardSolver()
                                                    .processEdge(new PathEdge<Unit, Abstraction>(d1, u, newAbs));
                                    }
                                }
                            }
                        }

                        return res;
                    }
                };
            }
            @Override
            public FlowFunction<Abstraction> getCallFlowFunction(final Unit callSite, final SootMethod dest) {
                if (!dest.isConcrete()) {
                    logger.debug("Call skipped because target has no body: {} -> {}", callSite, dest);
                    return KillAll.v();
                }

                if (!(callSite instanceof Stmt))
                    return KillAll.v();

                final Stmt stmt = (Stmt) callSite;
                final InvokeExpr ie = stmt.containsInvokeExpr() ? stmt.getInvokeExpr() : null;

                final Local[] paramLocals = dest.getActiveBody().getParameterLocals().toArray(new Local[0]);
                final Local thisLocal = dest.isStatic() ? null : dest.getActiveBody().getThisLocal();

                final boolean isSource = manager.getSourceSinkManager() != null
                        && manager.getSourceSinkManager().getSourceInfo((Stmt) callSite, manager) != null;
                final boolean isSink = manager.getSourceSinkManager() != null
                        && manager.getSourceSinkManager().getSinkInfo(stmt, manager, null) != null;

                final boolean isExecutorExecute = interproceduralCFG().isExecutorExecute(ie, dest);
                final boolean isReflectiveCallSite = interproceduralCFG().isReflectiveCallSite(ie);

                return new SolverCallFlowFunction() {
                    @Override
                    public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
                        if (source == getZeroValue())
                            return null;

                        // Notify the handler if we have one
                        if (taintPropagationHandler != null)
                            taintPropagationHandler.notifyFlowIn(stmt, source, manager,
                                    TaintPropagationHandler.FlowFunctionType.CallFlowFunction);

                        // TurnUnit is the sink. Below this stmt, the taint is not valid anymore
                        // Therefore we turn around here.
                        if (source.getTurnUnit() == callSite) {
                            return notifyOutFlowHandlers(callSite, d1, source, null,
                                    TaintPropagationHandler.FlowFunctionType.CallFlowFunction);
                        }

                        Set<Abstraction> res = computeTargetsInternal(d1,source);
                        if (DEBUG_PRINT)
                            System.out.println("Alias Call" + "\n" + "In: " + source.toString() + "\n" + "Stmt: " + stmt.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n" + "---------------------------------------");

                        return notifyOutFlowHandlers(stmt, d1, source, res,
                                TaintPropagationHandler.FlowFunctionType.CallFlowFunction);
                    }
                    private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
                        if (!manager.getConfig().getInspectSources() && isSource)
                            return null;
                        if (!manager.getConfig().getInspectSinks() && isSink)
                            return null;
                        if (manager.getConfig().getStaticFieldTrackingMode() == InfoflowConfiguration.StaticFieldTrackingMode.None
                                && dest.isStaticInitializer())
                            return null;
                        if (isExcluded(dest))
                            return null;

                        if (manager.getConfig().getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
                                && source.getAccessPath().isStaticFieldRef()) {
                            if (!interproceduralCFG().isStaticFieldRead(dest, source.getAccessPath().getFirstField()))
                                return null;
                        }

                        HashSet<Abstraction> res = new HashSet<>();

                        if (manager.getConfig().getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
                                && source.getAccessPath().isStaticFieldRef()) {
                            Abstraction abs = checkAbstraction(
                                    source.deriveNewAbstraction(source.getAccessPath(), stmt));
                            if (abs != null)
                                res.add(abs);
                        }

                        // map o to this
                        if (!isExecutorExecute && !source.getAccessPath().isStaticFieldRef() && !dest.isStatic()) {
                            InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
                            Value callBase = isReflectiveCallSite ?
                                    instanceInvokeExpr.getArg(0) : instanceInvokeExpr.getBase();

                            Value sourceBase = source.getAccessPath().getPlainValue();
                            if (callBase == sourceBase && manager.getTypeUtils()
                                    .hasCompatibleTypesForCall(source.getAccessPath(), dest.getDeclaringClass())) {
                                if (isReflectiveCallSite ||
                                        instanceInvokeExpr.getArgs().stream().noneMatch(arg -> arg == sourceBase)) {
                                    AccessPath ap = manager.getAccessPathFactory()
                                            .copyWithNewValue(source.getAccessPath(), thisLocal);
                                    Abstraction abs = checkAbstraction(source.deriveNewAbstraction(ap, (Stmt) callSite));
                                    if (abs != null)
                                        res.add(abs);
                                }
                            }
                        }

                        // map arguments to parameter
                        if (isExecutorExecute && ie != null && ie.getArg(0) == source.getAccessPath().getPlainValue()) {
                            AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                    thisLocal);
                            Abstraction abs = checkAbstraction(source.deriveNewAbstraction(ap, stmt));
                            if (abs != null)
                                res.add(abs);
                        } else if (ie != null && dest.getParameterCount() > 0) {
                            for (int i = isReflectiveCallSite ? 1 : 0; i < ie.getArgCount(); i++) {
                                if (ie.getArg(i) != source.getAccessPath().getPlainValue())
                                    continue;

                                // taint all parameters if reflective call site
                                if (isReflectiveCallSite) {
                                    for (Value param : paramLocals) {
                                        AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(
                                                source.getAccessPath(), param, null, false);
                                        Abstraction abs = checkAbstraction(source.deriveNewAbstraction(ap, stmt));
                                        if (abs != null)
                                            res.add(abs);
                                    }
                                    // taint just the tainted parameter
                                } else {
                                    AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(
                                            source.getAccessPath(), paramLocals[i]);
                                    Abstraction abs = checkAbstraction(source.deriveNewAbstraction(ap, stmt));
                                    if (abs != null)
                                        res.add(abs);
                                }
                            }
                        }

                        if (res != null) {
                            for (Abstraction d3 : res)
                                manager.getForwardSolver().injectContext(solver, dest, d3, callSite, source, d1);
                        }
                        return res;
                    }
                };
            }

            @Override
            public FlowFunction<Abstraction> getReturnFlowFunction(Unit callSite, SootMethod callee, Unit exitStmt, Unit returnSite) {
                if (callSite != null && !(callSite instanceof Stmt))
                    return KillAll.v();

                final Value[] paramLocals = new Value[callee.getParameterCount()];
                for (int i = 0; i < callee.getParameterCount(); i++)
                    paramLocals[i] = callee.getActiveBody().getParameterLocal(i);

                final Stmt callStmt = (Stmt) callSite;
                final InvokeExpr ie = (callStmt != null && callStmt.containsInvokeExpr()) ? callStmt.getInvokeExpr() : null;
                final boolean isReflectiveCallSite = interproceduralCFG().isReflectiveCallSite(ie);
                final ReturnStmt returnStmt = (exitStmt instanceof ReturnStmt) ? (ReturnStmt) exitStmt : null;

                final Local thisLocal = callee.isStatic() ? null : callee.getActiveBody().getThisLocal();
                final boolean isExecutorExecute = interproceduralCFG().isExecutorExecute(ie, callee);

                return new SolverReturnFlowFunction() {
                    @Override
                    public Set<Abstraction> computeTargets(Abstraction source, Abstraction calleeD1, Collection<Abstraction> callerD1s) {
                        if (source == getZeroValue())
                            return null;
                        if (callSite == null)
                            return null;

                        if (taintPropagationHandler != null)
                            taintPropagationHandler.notifyFlowIn(callStmt, source, manager,
                                    TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction);

                        // TurnUnit is the sink. Below this stmt, the taint is not valid anymore
                        // Therefore we turn around here.
                        if (source.getTurnUnit() == callSite) {
                            return notifyOutFlowHandlers(callSite, calleeD1, source, null,
                                    TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction);
                        }

                        Set<Abstraction> res = computeTargetsInternal(source, calleeD1, callerD1s);
                        if (DEBUG_PRINT)
                            System.out.println("Alias Return" + "\n" + "In: " + source.toString() + "\n" + "Stmt: " + callSite.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n" + "---------------------------------------");

                        return notifyOutFlowHandlers(exitStmt, calleeD1, source, res,
                                TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction);
                    }

                    private Set<Abstraction> computeTargetsInternal(Abstraction source, Abstraction calleeD1, Collection<Abstraction> callerD1s) {
                        HashSet<Abstraction> res = new HashSet<>();

                        // Static fields get propagated unchaged
                        if (manager.getConfig().getStaticFieldTrackingMode()
                                != InfoflowConfiguration.StaticFieldTrackingMode.None
                                && source.getAccessPath().isStaticFieldRef()) {
                            registerActivationCallSite(callSite, callee, source);
                            res.add(source);
                            return res;
                        }

                        // x = o.m()
                        if (!source.getAccessPath().isInstanceFieldRef() && !callee.isStatic()
                                && returnStmt != null && callStmt instanceof AssignStmt) {
                            Value retLocal = returnStmt.getOp();
                            DefinitionStmt defnStmt = (DefinitionStmt) callSite;
                            Value leftOp = defnStmt.getLeftOp();

                            if (retLocal == source.getAccessPath().getPlainValue()
                                    && !isExceptionHandler(returnSite)) {
                                AccessPath ap = manager.getAccessPathFactory()
                                        .copyWithNewValue(source.getAccessPath(), leftOp);
                                Abstraction abs = checkAbstraction(source.deriveNewAbstraction(ap, (Stmt) exitStmt));
                                if (abs != null) {
                                    res.add(abs);
                                }
                            }
                        }


                        // o.m(a1, ..., an)
                        // map o.f to this.f
                        if (!isExecutorExecute && !callee.isStatic()) {
                            Value sourceBase = source.getAccessPath().getPlainValue();
                            if (thisLocal == sourceBase && manager.getTypeUtils()
                                    .hasCompatibleTypesForCall(source.getAccessPath(), callee.getDeclaringClass())) {
                                InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) callStmt.getInvokeExpr();
                                Value callBase = isReflectiveCallSite ?
                                        instanceInvokeExpr.getArg(0) : instanceInvokeExpr.getBase();

                                // TODO: understand why second condition
                                if (isReflectiveCallSite ||
                                        instanceInvokeExpr.getArgs().stream().noneMatch(arg -> arg == sourceBase)) {
                                    AccessPath ap = manager.getAccessPathFactory()
                                            .copyWithNewValue(source.getAccessPath(), callBase, isReflectiveCallSite ? null
                                                    : source.getAccessPath().getBaseType(),false);
                                    Abstraction abs = checkAbstraction(source.deriveNewAbstraction(ap, (Stmt) exitStmt));
                                    if (abs != null) {
                                        registerActivationCallSite(callSite, callee, abs);
                                        res.add(abs);
                                    }
                                }
                            }
                        }

                        // map arguments to parameter
                        if (isExecutorExecute && ie != null && ie.getArg(0) == source.getAccessPath().getPlainValue()) {
                            AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                    thisLocal);
                            Abstraction abs = checkAbstraction(source.deriveNewAbstraction(ap, callStmt));
                            if (abs != null) {
                                registerActivationCallSite(callSite, callee, source);
                                res.add(abs);
                            }
                        } else if (ie != null) {
                            for (int i = 0; i < callee.getParameterCount(); i++) {
                                Value originalCallArg = ie.getArg(isReflectiveCallSite ? 1 : i);

                                if (!AccessPath.canContainValue(originalCallArg))
                                    continue;
                                if (!isReflectiveCallSite && !manager.getTypeUtils()
                                        .checkCast(source.getAccessPath(), originalCallArg.getType()))
                                    continue;
                                if (isPrimtiveOrStringBase(source))
                                    continue;

                                AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(
                                        source.getAccessPath(), originalCallArg,
                                        isReflectiveCallSite ? null : source.getAccessPath().getBaseType(),
                                        false);
                                Abstraction abs = checkAbstraction(source.deriveNewAbstraction(ap, (Stmt) exitStmt));
                                if (abs != null) {
                                    registerActivationCallSite(callSite, callee, abs);
                                    res.add(abs);
                                }
                            }
                        }

                        return res;
                    }
                };
            }

            @Override
            public FlowFunction<Abstraction> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
                if (!(callSite instanceof Stmt)) {
                    return KillAll.v();
                }

                final Stmt callStmt = (Stmt) callSite;
                final InvokeExpr invExpr = callStmt.getInvokeExpr();

                final Value[] callArgs = new Value[invExpr.getArgCount()];
                for (int i = 0; i < invExpr.getArgCount(); i++) {
                    callArgs[i] = invExpr.getArg(i);
                }

                final SootMethod callee = invExpr.getMethod();

                final boolean isSink = manager.getSourceSinkManager() != null
                        && manager.getSourceSinkManager().getSinkInfo(callStmt, manager, null) != null;
                final boolean isSource = manager.getSourceSinkManager() != null
                        && manager.getSourceSinkManager().getSourceInfo(callStmt, manager) != null;

                return new SolverCallToReturnFlowFunction() {
                    @Override
                    public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
                        if (source == getZeroValue())
                            return null;

                        // Notify the handler if we have one
                        if (taintPropagationHandler != null)
                            taintPropagationHandler.notifyFlowIn(callSite, source, manager,
                                    TaintPropagationHandler.FlowFunctionType.CallToReturnFlowFunction);

                        // TurnUnit is the sink. Below this stmt, the taint is not valid anymore
                        // Therefore we turn around here.
                        if (source.getTurnUnit() == callSite) {
                            for (Unit u : interproceduralCFG().getPredsOf(callSite))
                                manager.getForwardSolver()
                                        .processEdge(new PathEdge<Unit, Abstraction>(d1, u, source));

                            return notifyOutFlowHandlers(callSite, d1, source, null,
                                    TaintPropagationHandler.FlowFunctionType.CallToReturnFlowFunction);
                        }

                        Set<Abstraction> res = computeTargetsInternal(d1, source);
                        if (DEBUG_PRINT)
                            System.out.println("Alias CallToReturn" + "\n" + "In: " + source.toString() + "\n" + "Stmt: " + callStmt.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n" + "---------------------------------------");

                        return notifyOutFlowHandlers(callSite, d1, source, res,
                                TaintPropagationHandler.FlowFunctionType.CallToReturnFlowFunction);
                    }

                    private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
                        HashSet<Abstraction> res = new HashSet<>();
                        res.add(source);

                        // If excluded or we do not anything about the callee,
                        // we just pass the taint over the statement
                        if (isExcluded(callee) || interproceduralCFG().getCalleesOfCallAt(callSite).isEmpty())
                            return res;

                        // If static field is used, we do not pass it over
                        if (manager.getConfig().getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
                                && source.getAccessPath().isStaticFieldRef()
                                && interproceduralCFG().isStaticFieldUsed(callee, source.getAccessPath().getFirstField())) {
                                return null;
                        }

                        // Do not pass tainted base over the statement
                        if (callStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
                            InstanceInvokeExpr inv = (InstanceInvokeExpr) callStmt.getInvokeExpr();
                            if (inv.getBase() == source.getAccessPath().getPlainValue())
                                return null;
                        }

                        // Do not pass over reference parameters
                        // CallFlow passes this into the callee
                        if (Arrays.stream(callArgs).anyMatch(arg -> !isPrimtiveOrStringBase(source)
                                && arg == source.getAccessPath().getPlainValue()))
                            return null;

                        return res;
                    }
                };
            }

            private boolean isPrimtiveOrStringBase(Abstraction abs) {
                Type t = abs.getAccessPath().getBaseType();
                return t instanceof PrimType || TypeUtils.isStringType(t);
            }
        };
    }
}
