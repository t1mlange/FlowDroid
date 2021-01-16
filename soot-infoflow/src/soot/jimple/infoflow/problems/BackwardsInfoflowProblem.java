package soot.jimple.infoflow.problems;

import heros.FlowFunction;
import heros.FlowFunctions;
import heros.flowfunc.KillAll;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;
import soot.jimple.infoflow.problems.rules.PropagationRuleManager;
import soot.jimple.infoflow.solver.functions.SolverCallFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverCallToReturnFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverNormalFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverReturnFlowFunction;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.TypeUtils;

import java.util.*;

/**
 * Class which contains the flow functions for the backwards analysis.
 * Not to be confused with the BackwardsAliasProblem, which is used for finding aliases.
 *
 * @author Tim Lange
 */
public class BackwardsInfoflowProblem extends AbstractInfoflowProblem {
    private final static boolean DEBUG_PRINT = false;
    private final static boolean ONLY_CALLS = true;

    private final PropagationRuleManager propagationRules;
    protected final TaintPropagationResults results;

    public BackwardsInfoflowProblem(InfoflowManager manager,
                                    Abstraction zeroValue, IPropagationRuleManagerFactory ruleManagerFactory) {
        super(manager);

        this.zeroValue = zeroValue == null ? createZeroValue() : zeroValue;
        this.results = new TaintPropagationResults(manager);
        this.propagationRules = ruleManagerFactory.createRuleManager(manager, this.zeroValue, results);
    }

    @Override
    protected FlowFunctions<Unit, Abstraction, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Unit, Abstraction, SootMethod>() {

            @Override
            public FlowFunction<Abstraction> getNormalFlowFunction(Unit srcUnit, Unit destUnit) {
                if (!(srcUnit instanceof Stmt))
                    return KillAll.v();

                final Aliasing aliasing = manager.getAliasing();
                if (aliasing == null)
                    return null;

                return new SolverNormalFlowFunction() {
                    @Override
                    public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
                        if (taintPropagationHandler != null)
                            taintPropagationHandler.notifyFlowIn(srcUnit, source, manager,
                                    TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);

                        Set<Abstraction> res = computeTargetsInternal(d1, source.isAbstractionActive() ? source : source.getActiveCopy());
                        if (DEBUG_PRINT && !ONLY_CALLS)
                            System.out.println("Normal" + "\n" + "In: " + source.toString() + "\n" + "Stmt: " + srcUnit.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n" + "---------------------------------------");

                        return notifyOutFlowHandlers(srcUnit, d1, source, res,
                                TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);
                    }

                    private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
                        Set<Abstraction> res = null;
                        ByReferenceBoolean killSource = new ByReferenceBoolean();
                        ByReferenceBoolean killAll = new ByReferenceBoolean();
                        // If we have a RuleManager, apply the rules
                        if (propagationRules != null) {
                            res = propagationRules.applyNormalFlowFunction(d1, source, (Stmt) srcUnit,
                                    (Stmt) destUnit, killSource, killAll);
                        }
                        // On killAll, we do not propagate anything and can stop here
                        if (killAll.value)
                            return null;

                        // Instanciate res in case the RuleManager did return null
                        if (res == null)
                            res = new HashSet<>();

                        if (!(srcUnit instanceof AssignStmt))
                            return res;

                        final AssignStmt assignStmt = (AssignStmt) srcUnit;
                        // left can not be an expr
                        final Value leftVal = assignStmt.getLeftOp();
                        final Value rightOp = assignStmt.getRightOp();
                        final Value[] rightVals = BaseSelector.selectBaseList(rightOp, true);

                        AccessPath ap = source.getAccessPath();
                        Local sourceBase = ap.getPlainValue();
                        // Statements such as c = a + b with the taint c can produce multiple taints because we can not
                        // decide which one originated from a source at this point.
                        for (Value rightVal : rightVals) {
                            if (source.getSkipUnit() != srcUnit) {
                                boolean addLeftValue = false;
                                boolean cutFirstFieldLeft = false;
                                boolean createNewVal = false;
                                Type leftType = null;

                                if (rightVal instanceof StaticFieldRef) {
                                    StaticFieldRef staticRef = (StaticFieldRef) rightVal;

                                    if (manager.getConfig().getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
                                            && ap.firstFieldMatches(staticRef.getField())) {
                                        addLeftValue = true;
                                        cutFirstFieldLeft = true;
//                                    leftType = source.getAccessPath().getBaseType();
                                    }
                                } else if (rightVal instanceof InstanceFieldRef) {
                                    InstanceFieldRef instRef = (InstanceFieldRef) rightVal;

                                    if (aliasing.mayAlias(instRef.getBase(), sourceBase)) {
                                        if (ap.firstFieldMatches(instRef.getField())) {
                                            addLeftValue = true;
                                            cutFirstFieldLeft = ap.getFieldCount() > 0
                                                    && ap.getFirstField() == instRef.getField();
//                                    leftType = ap.getFirstFieldType();
                                        } else if (ap.getTaintSubFields() && ap.getFieldCount() == 0) {
                                            addLeftValue = true;
                                        } else if (source.dependsOnCutAP() && !(leftVal.getType() instanceof PrimType)) {
                                            addLeftValue = true;
                                            cutFirstFieldLeft = true;
                                        }
                                    }
                                } else if (rightVal instanceof ArrayRef) {
                                    if (!getManager().getConfig().getEnableArrayTracking()
                                            || ap.getArrayTaintType() == AccessPath.ArrayTaintType.Length)
                                        continue;

                                    ArrayRef arrayRef = (ArrayRef) rightVal;
                                    // do we track indices...
                                    if (getManager().getConfig().getImplicitFlowMode().trackArrayAccesses()) {
                                        if (arrayRef.getIndex() == sourceBase) {
                                            addLeftValue = true;
                                            leftType = ((ArrayType) arrayRef.getBase().getType()).getElementType();
                                        }
                                    }
                                    // ...or only the whole array?
                                    else if (aliasing.mayAlias(arrayRef.getBase(), sourceBase)) {
                                        addLeftValue = true;
                                        leftType = ((ArrayType) arrayRef.getBase().getType()).getElementType();
                                    }
                                }
                                if (rightVal == sourceBase) {
                                    addLeftValue = true;
                                    leftType = ap.getBaseType();

                                    if (leftVal instanceof ArrayRef) {
                                        ArrayRef arrayRef = (ArrayRef) leftVal;
                                        leftType = TypeUtils.buildArrayOrAddDimension(leftType, arrayRef.getType().getArrayType());
                                    } else if (rightOp instanceof InstanceOfExpr) {
                                        createNewVal = true;
                                    } else if (rightOp instanceof LengthExpr) {
                                        if (ap.getArrayTaintType() == AccessPath.ArrayTaintType.Contents)
                                            addLeftValue = false;
                                        createNewVal = true;
                                    } else if (rightOp instanceof NewArrayExpr) {
                                        createNewVal = true;
                                    } else {
                                        if (!manager.getTypeUtils().checkCast(source.getAccessPath(), leftVal.getType()))
                                            return null;
                                    }

                                    if (rightVal instanceof CastExpr) {
                                        CastExpr ce = (CastExpr) rightOp;
                                        if (!manager.getHierarchy().canStoreType(leftType, ce.getCastType()))
                                            leftType = ce.getCastType();
                                    }
                                }

                                if (addLeftValue) {
                                    AccessPath newAp;
                                    if (createNewVal)
                                        newAp = manager.getAccessPathFactory().createAccessPath(leftVal, true);
                                    else
                                        newAp = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                                leftVal, leftType, cutFirstFieldLeft);
                                    Abstraction newAbs = source.deriveNewAbstraction(newAp, assignStmt);
                                    if (newAbs != null) {
                                        if (aliasing.canHaveAliasesRightSide(assignStmt, leftVal, newAbs)) {
                                            aliasing.computeAliases(d1, assignStmt, leftVal, res,
                                                    interproceduralCFG().getMethodOf(assignStmt), newAbs);
                                        }
                                    }
                                }
                            }

                            // NewExpr's can not be tainted
                            // so we can stop here
                            if (rightOp instanceof AnyNewExpr)
                                continue;

                            boolean addRightValue = false;
                            boolean cutFirstField = false;
                            Type rightType = null;

                            // S.x
                            if (leftVal instanceof StaticFieldRef) {
                                // If we do not track statics, just skip this rightVal
                                if (getManager().getConfig().getStaticFieldTrackingMode()
                                        != InfoflowConfiguration.StaticFieldTrackingMode.None
                                        && ap.firstFieldMatches(((StaticFieldRef) leftVal).getField())) {
                                    addRightValue = true;
                                    cutFirstField = true;
                                    rightType = ap.getFirstFieldType();
                                }
                            }
                            // o.x
                            else if (leftVal instanceof InstanceFieldRef) {
                                InstanceFieldRef instRef = ((InstanceFieldRef) leftVal);

                                // Kill the taint if o = null
                                if (instRef.getBase().getType() instanceof NullType)
                                    return null;

                                // base object matches
                                if (aliasing.mayAlias(instRef.getBase(), sourceBase)) {
                                    // field also matches
                                    if (ap.firstFieldMatches(instRef.getField())) {
                                        addRightValue = true;
                                        // Without o1.x = o2.x would result in o2.x.x
                                        cutFirstField = true;
                                        rightType = ap.getFirstFieldType();
                                    }
                                    // whole object is tainted
                                    else if (ap.getTaintSubFields() && ap.getFieldCount() == 0) {
                                        addRightValue = true;
                                        rightType = instRef.getField().getType();
                                    } else if (source.dependsOnCutAP() && !(rightVal.getType() instanceof PrimType)) {
                                        addRightValue = true;
                                        cutFirstField = true;
                                    }
                                }
                            } else if (leftVal instanceof ArrayRef) {
                                // If we don't track arrays or just the length is tainted we have nothing to do.
                                if (!getManager().getConfig().getEnableArrayTracking()
                                        || ap.getArrayTaintType() == AccessPath.ArrayTaintType.Length)
                                    continue;

                                ArrayRef arrayRef = (ArrayRef) leftVal;
                                // do we track indices...
                                if (getManager().getConfig().getImplicitFlowMode().trackArrayAccesses()) {
                                    if (arrayRef.getIndex() == sourceBase) {
                                        addRightValue = true;
                                        rightType = ((ArrayType) arrayRef.getBase().getType()).getElementType();
                                    }
                                }
                                // ...or only the whole array?
                                else if (aliasing.mayAlias(arrayRef.getBase(), sourceBase)) {
                                    addRightValue = true;
                                    rightType = ((ArrayType) arrayRef.getBase().getType()).getElementType();
                                }
                            }
                            // default case
                            else if (aliasing.mayAlias(leftVal, sourceBase)) {
                                if (rightOp instanceof InstanceOfExpr) {
                                    // Left side is a boolean but the resulting taint
                                    // needs to be the object type
                                    rightType = rightVal.getType();
                                } else if (rightOp instanceof CastExpr) {
                                    // CastExpr only make the types more imprecise backwards
                                    // but we need to kill impossible casts
                                    CastExpr ce = (CastExpr) rightOp;
                                    if (!manager.getTypeUtils().checkCast(ce.getCastType(), rightVal.getType()))
                                        return null;
                                }
                                // LengthExpr/RHS ArrayRef handled in ArrayPropagationRule
                                addRightValue = !(rightOp instanceof LengthExpr || rightOp instanceof ArrayRef);
                            }

                            if (addRightValue) {
                                // TODO: Don't kill on whole object?
                                if (!(leftVal instanceof ArrayRef)
                                        || getManager().getConfig().getImplicitFlowMode().trackArrayAccesses()
                                        || (ap.getTaintSubFields() && ap.getFieldCount() == 0 && rightVal instanceof FieldRef))
                                    res.remove(source);

                                if (rightVal instanceof Constant)
                                    continue;

                                AccessPath newAp = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                        rightVal, rightType, cutFirstField);
                                Abstraction newAbs = source.deriveNewAbstraction(newAp, assignStmt);
                                if (newAbs != null) {
                                    if (rightVal instanceof StaticFieldRef && manager.getConfig().getStaticFieldTrackingMode()
                                            == InfoflowConfiguration.StaticFieldTrackingMode.ContextFlowInsensitive)
                                        manager.getGlobalTaintManager().addToGlobalTaintState(newAbs);
                                    else {
                                        res.add(newAbs);

                                        if (!isPrimtiveOrStringBase(source) && aliasing.canHaveAliasesRightSide(assignStmt, rightVal, newAbs)) {
                                            aliasing.computeAliases(d1, assignStmt, rightVal, res,
                                                    interproceduralCFG().getMethodOf(assignStmt), newAbs);
                                        }
                                    }
                                }
                            }
                        }

                        return res;
                    }
                };
            }

            @Override
            public FlowFunction<Abstraction> getCallFlowFunction(final Unit callStmt,
                                                                 final SootMethod dest) {
                if (!dest.isConcrete()) {
                    logger.debug("Call skipped because target has no body: {} -> {}", callStmt, dest);
                    return KillAll.v();
                }

                final Aliasing aliasing = manager.getAliasing();
                if (aliasing == null)
                    return null;

                if (!(callStmt instanceof Stmt))
                    return KillAll.v();

                final Stmt stmt = (Stmt) callStmt;
                final InvokeExpr ie = stmt.containsInvokeExpr() ? stmt.getInvokeExpr() : null;

                final Local[] paramLocals = dest.getActiveBody().getParameterLocals().toArray(new Local[0]);
                final Local thisLocal = dest.isStatic() ? null : dest.getActiveBody().getThisLocal();

                final boolean isSource = manager.getSourceSinkManager() != null
                        && manager.getSourceSinkManager().getSourceInfo((Stmt) callStmt, manager) != null;
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

                        Set<Abstraction> res = computeTargetsInternal(d1, source.isAbstractionActive() ? source : source.getActiveCopy());
                        if (res != null) {
                            for (Abstraction abs : res)
                                aliasing.getAliasingStrategy().injectCallingContext(abs, solver, dest, callStmt, source, d1);
                        }
                        if (DEBUG_PRINT)
                            System.out.println("Call" + "\n" + "In: " + source.toString() + "\n" + "Stmt: " + stmt.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n" + "---------------------------------------");

                        return notifyOutFlowHandlers(stmt, d1, source, res,
                                TaintPropagationHandler.FlowFunctionType.CallFlowFunction);
                    }

                    private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
                        // Respect user settings
                        if (manager.getConfig().getStopAfterFirstFlow() && !results.isEmpty())
                            return null;
                        if (!manager.getConfig().getInspectSources() && isSource)
                            return null;
                        if (!manager.getConfig().getInspectSinks() && isSink)
                            return null;
                        if (manager.getConfig().getStaticFieldTrackingMode() ==
                                InfoflowConfiguration.StaticFieldTrackingMode.None && dest.isStaticInitializer())
                            return null;

                        // Do not propagate into Soot library classes if that optimization is enabled
                        // CallToReturn handles the propagation over the excluded statement
                        if (isExcluded(dest))
                            return null;

                        // not used static fields do not need to be propagated
                        if (manager.getConfig().getStaticFieldTrackingMode()
                                != InfoflowConfiguration.StaticFieldTrackingMode.None
                                && source.getAccessPath().isStaticFieldRef()) {
                            // static fields first get pushed onto the stack before used,
                            // so we check for a read on the base class
                            if (!(interproceduralCFG().isStaticFieldUsed(dest, source.getAccessPath().getFirstField())
                                    || interproceduralCFG().isStaticFieldRead(dest, source.getAccessPath().getFirstField())))
                                return null;
                        }

                        Set<Abstraction> res = null;
                        ByReferenceBoolean killAll = new ByReferenceBoolean();
                        if (propagationRules != null)
                            res = propagationRules.applyCallFlowFunction(d1, source, stmt, dest, killAll);
                        if (killAll.value)
                            return null;

                        // Instanciate in case RuleManager did not produce an object
                        if (res == null)
                            res = new HashSet<>();

                        // x = o.m(a1, ..., an)
                        // Taints the return if needed
                        if (callStmt instanceof AssignStmt) {
                            AssignStmt assignStmt = (AssignStmt) callStmt;
                            Value left = assignStmt.getLeftOp();

                            // we only taint the return statement(s) if x is tainted
                            if (aliasing.mayAlias(left, source.getAccessPath().getPlainValue())) {
                                for (Unit unit : dest.getActiveBody().getUnits()) {
                                    if (unit instanceof ReturnStmt) {
                                        ReturnStmt returnStmt = (ReturnStmt) unit;
                                        Value retVal = returnStmt.getOp();
                                        // taint only if local variable or reference
                                        if (retVal instanceof Local || retVal instanceof FieldRef) {
                                            // if types are incompatible, stop here
                                            if (!manager.getTypeUtils().checkCast(source.getAccessPath().getBaseType(), retVal.getType()))
                                                continue;

                                            AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(
                                                    source.getAccessPath(), retVal, returnStmt.getOp().getType(), false);
                                            Abstraction abs = source.deriveNewAbstraction(ap, stmt);
                                            if (abs != null) {
                                                res.add(abs);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // static fields access path stay the same
                        if (manager.getConfig().getStaticFieldTrackingMode() !=
                                InfoflowConfiguration.StaticFieldTrackingMode.None
                                && source.getAccessPath().isStaticFieldRef()) {
                            Abstraction abs = source.deriveNewAbstraction(source.getAccessPath(), stmt);
                            if (abs != null)
                                res.add(abs);
                        }

                        // o.m(a1, ..., an)
                        // map o.f to this.f
                        if (!isExecutorExecute && !source.getAccessPath().isStaticFieldRef() && !dest.isStatic()) {
                            InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
                            Value callBase = isReflectiveCallSite ?
                                    instanceInvokeExpr.getArg(0) : instanceInvokeExpr.getBase();

                            Value sourceBase = source.getAccessPath().getPlainValue();
                            if (aliasing.mayAlias(callBase, sourceBase) && manager.getTypeUtils()
                                    .hasCompatibleTypesForCall(source.getAccessPath(), dest.getDeclaringClass())) {
                                // TODO: understand why second condition
                                if (isReflectiveCallSite ||
                                        instanceInvokeExpr.getArgs().stream().noneMatch(arg -> arg == sourceBase)) {
                                    AccessPath ap = manager.getAccessPathFactory()
                                            .copyWithNewValue(source.getAccessPath(), thisLocal);
                                    Abstraction abs = source.deriveNewAbstraction(ap, (Stmt) callStmt);
                                    if (abs != null)
                                        res.add(abs);
                                }
                            }
                        }

                        // map arguments to parameter
                        if (isExecutorExecute) {
                            if (ie != null && aliasing.mayAlias(ie.getArg(0), source.getAccessPath().getPlainValue())) {
                                AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                        thisLocal);
                                Abstraction abs = source.deriveNewAbstraction(ap, stmt);
                                if (abs != null)
                                    res.add(abs);
                            }
                        } else if (ie != null && dest.getParameterCount() > 0) {
                            for (int i = isReflectiveCallSite ? 1 : 0; i < ie.getArgCount(); i++) {
                                if (!aliasing.mayAlias(ie.getArg(i), source.getAccessPath().getPlainValue()))
                                    continue;

                                // taint all parameters if reflective call site
                                if (isReflectiveCallSite) {
                                    for (Value param : paramLocals) {
                                        AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(
                                                source.getAccessPath(), param, null, false);
                                        Abstraction abs = source.deriveNewAbstraction(ap, stmt);
                                        if (abs != null)
                                            res.add(abs);
                                    }
                                    // taint just the tainted parameter
                                } else {
                                    AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(
                                            source.getAccessPath(), paramLocals[i]);
                                    Abstraction abs = source.deriveNewAbstraction(ap, stmt);
                                    if (abs != null)
                                        res.add(abs);
                                }
                            }
                        }

                        return res;
                    }
                };
            }

            @Override
            public FlowFunction<Abstraction> getReturnFlowFunction(Unit callSite, SootMethod callee, Unit
                    exitSite, Unit returnSite) {
                if (callSite != null && !(callSite instanceof Stmt))
                    return KillAll.v();

                final Aliasing aliasing = manager.getAliasing();
                if (aliasing == null)
                    return null;

                final Value[] paramLocals = new Value[callee.getParameterCount()];
                for (int i = 0; i < callee.getParameterCount(); i++)
                    paramLocals[i] = callee.getActiveBody().getParameterLocal(i);

                final Stmt stmt = (Stmt) callSite;
                final InvokeExpr ie = (stmt != null && stmt.containsInvokeExpr()) ? stmt.getInvokeExpr() : null;
                final boolean isReflectiveCallSite = interproceduralCFG().isReflectiveCallSite(ie);
                final Stmt callStmt = (Stmt) callSite;
                final Stmt exitStmt = (Stmt) exitSite;
//                final ReturnStmt returnStmt = (exitSite instanceof ReturnStmt) ? (ReturnStmt) exitSite : null;

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
                            taintPropagationHandler.notifyFlowIn(stmt, source, manager,
                                    TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction);

                        Set<Abstraction> res = computeTargetsInternal(source.isAbstractionActive() ? source : source.getActiveCopy(), callerD1s);
                        if (DEBUG_PRINT)
                            System.out.println("Return" + "\n" + "In: " + source.toString() + "\n" + "Stmt: " + stmt.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n" + "---------------------------------------");
                        return notifyOutFlowHandlers(exitSite, calleeD1, source, res,
                                TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction);
                    }

                    private Set<Abstraction> computeTargetsInternal(Abstraction source, Collection<Abstraction> callerD1s) {
                        if (manager.getConfig().getStopAfterFirstFlow() && !results.isEmpty())
                            return null;

                        Set<Abstraction> res = null;
                        ByReferenceBoolean killAll = new ByReferenceBoolean();
                        if (propagationRules != null)
                            res = propagationRules.applyReturnFlowFunction(callerD1s, source,
                                    (Stmt) exitSite, (Stmt) returnSite, (Stmt) callSite, killAll);
                        if (killAll.value)
                            return null;

                        if (res == null)
                            res = new HashSet<>();

                        // Static fields get propagated unchaged
                        if (manager.getConfig().getStaticFieldTrackingMode()
                                != InfoflowConfiguration.StaticFieldTrackingMode.None
                                && source.getAccessPath().isStaticFieldRef()) {
                            res.add(source);
                        }

                        // o.m(a1, ..., an)
                        // map o.f to this.f
                        if (!isExecutorExecute && !callee.isStatic()) {
                            Value sourceBase = source.getAccessPath().getPlainValue();
                            if (aliasing.mayAlias(thisLocal, sourceBase) && manager.getTypeUtils()
                                    .hasCompatibleTypesForCall(source.getAccessPath(), callee.getDeclaringClass())) {
                                InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
                                Value callBase = isReflectiveCallSite ?
                                        instanceInvokeExpr.getArg(0) : instanceInvokeExpr.getBase();

                                // Either the callBase is from a reflective call site
                                // or the source base doesn't match with any parameters
                                if (isReflectiveCallSite ||
                                        instanceInvokeExpr.getArgs().stream().noneMatch(arg -> aliasing.mayAlias(arg, sourceBase))) {
                                    AccessPath ap = manager.getAccessPathFactory()
                                            .copyWithNewValue(source.getAccessPath(), callBase, isReflectiveCallSite ? null
                                                    : source.getAccessPath().getBaseType(), false);
                                    Abstraction abs = source.deriveNewAbstraction(ap, (Stmt) exitSite);
                                    if (abs != null) {
                                        res.add(abs);
                                    }
                                }
                            }
                        }

                        // map arguments to parameter
                        if (isExecutorExecute && ie != null) {
                            if (aliasing.mayAlias(thisLocal, source.getAccessPath().getPlainValue())) {
                                AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                        ie.getArg(0));
                                Abstraction abs = source.deriveNewAbstraction(ap, exitStmt);
                                if (abs != null) {
                                    abs.setCorrespondingCallSite(callStmt);
                                    res.add(abs);
                                }
                            }
                        } else if (ie != null) {
                            for (int i = 0; i < callee.getParameterCount(); i++) {
                                if (!aliasing.mayAlias(source.getAccessPath().getPlainValue(), paramLocals[i]))
                                    continue;
                                if (callSite instanceof AssignStmt) {
                                    // if parameter is
                                    if (aliasing.mayAlias(source.getAccessPath().getPlainValue(),
                                            ((AssignStmt) callSite).getLeftOp()))
                                        continue;
                                }

                                // Yes, we even map primitives or strings back as a
                                // return flow in backwards is a call.

                                Value originalCallArg = ie.getArg(isReflectiveCallSite ? 1 : i);
                                if (!AccessPath.canContainValue(originalCallArg))
                                    continue;
                                if (!isReflectiveCallSite && !manager.getTypeUtils()
                                        .checkCast(source.getAccessPath(), originalCallArg.getType()))
                                    continue;

                                AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(
                                        source.getAccessPath(), originalCallArg,
                                        isReflectiveCallSite ? null : source.getAccessPath().getBaseType(),
                                        false);
                                Abstraction abs = source.deriveNewAbstraction(ap, (Stmt) exitSite);
                                if (abs != null) {
                                    res.add(abs);


                                    if (callStmt instanceof AssignStmt) {
                                        Value leftOp = ((AssignStmt) callStmt).getLeftOp();

                                        if (!isPrimtiveOrStringType(leftOp.getType())
                                                && manager.getTypeUtils().checkCast(leftOp.getType(), abs.getAccessPath().getBaseType())
                                                && aliasing.canHaveAliasesRightSide((Stmt) callSite, originalCallArg, source)) {
                                            for (Abstraction callerD1 : callerD1s)
                                                aliasing.computeAliases(callerD1, (Stmt) callSite, originalCallArg, res, callee, abs);
                                        }
                                    }
                                }
                            }
                        }

                        setCallSite(source, res, (Stmt) callSite);
                        return res;
                    }
                };
            }

            @Override
            public FlowFunction<Abstraction> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
                if (!(callSite instanceof Stmt)) {
                    return KillAll.v();
                }

                final Aliasing aliasing = manager.getAliasing();
                if (aliasing == null)
                    return null;

                final Stmt callStmt = (Stmt) callSite;
                final InvokeExpr invExpr = callStmt.getInvokeExpr();

                final Value[] callArgs = new Value[invExpr.getArgCount()];
                for (int i = 0; i < invExpr.getArgCount(); i++) {
                    callArgs[i] = invExpr.getArg(i);
                }

                final SootMethod callee = invExpr.getMethod();

//                final boolean isSink = manager.getSourceSinkManager() != null
//                        && manager.getSourceSinkManager().getSinkInfo(callStmt, manager, null) != null;
//                final boolean isSource = manager.getSourceSinkManager() != null
//                        && manager.getSourceSinkManager().getSourceInfo(callStmt, manager) != null;

                return new SolverCallToReturnFlowFunction() {
                    @Override
                    public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
                        // Notify the handler if we have one
                        if (taintPropagationHandler != null)
                            taintPropagationHandler.notifyFlowIn(callSite, source, manager,
                                    TaintPropagationHandler.FlowFunctionType.CallToReturnFlowFunction);

                        Set<Abstraction> res = computeTargetsInternal(d1, source.isAbstractionActive() ? source : source.getActiveCopy());
                        if (DEBUG_PRINT)
                            System.out.println("CallToReturn" + "\n" + "In: " + source.toString() + "\n" + "Stmt: " + callStmt.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n" + "---------------------------------------");

                        return notifyOutFlowHandlers(callSite, d1, source, res,
                                TaintPropagationHandler.FlowFunctionType.CallToReturnFlowFunction);
                    }

                    private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
                        if (manager.getConfig().getStopAfterFirstFlow() && !results.isEmpty())
                            return null;

                        Set<Abstraction> res = null;
                        ByReferenceBoolean killSource = new ByReferenceBoolean();
                        ByReferenceBoolean killAll = new ByReferenceBoolean();
                        // if we have a RuleManager, apply the rules
                        if (propagationRules != null) {
                            res = propagationRules.applyCallToReturnFlowFunction(d1, source, callStmt,
                                    killSource, killAll, true);
                        }
                        // On killAll, we do not propagate and can stop here
                        if (killAll.value)
                            return null;

                        // Instanciate res if RuleManager did return null
                        if (res == null)
                            res = new HashSet<>();

                        if (isExcluded(callee)) {
                            res.add(source);
                            return res;
                        }

                        // If left side is tainted, the return value overwrites the taint
                        // CallFlow takes care of tainting the return value
                        if (callStmt instanceof AssignStmt
                                && aliasing.mayAlias(((AssignStmt) callStmt).getLeftOp(), source.getAccessPath().getPlainValue()))
                            return res;

                        // If we do not know the callees, we can not reason
                        if (interproceduralCFG().getCalleesOfCallAt(callSite).isEmpty()) {
                            if (source != zeroValue)
                                res.add(source);
                            return res;
                        }

                        // Static values can be propagated over methods if
                        // the value isn't written inside the method.
                        // Otherwise CallFlowFunction already does the job.
                        if (manager.getConfig().getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
                                && source.getAccessPath().isStaticFieldRef()
                                && interproceduralCFG().isStaticFieldUsed(callee, source.getAccessPath().getFirstField()))
                            return res;

                        if (callee.isNative() && ncHandler != null) {
                            for (Value arg : callArgs) {
                                if (aliasing.mayAlias(arg, source.getAccessPath().getPlainValue())) {
                                    Set<Abstraction> nativeAbs = ncHandler.getTaintedValues(callStmt, source, callArgs);
                                    if (nativeAbs != null) {
                                        res.addAll(nativeAbs);

                                        // Compute the aliases
                                        for (Abstraction abs : nativeAbs) {
                                            if (abs.getAccessPath().isStaticFieldRef() || aliasing.canHaveAliasesRightSide(
                                                    callStmt, abs.getAccessPath().getPlainValue(), abs)) {
                                                aliasing.computeAliases(d1, callStmt,
                                                        abs.getAccessPath().getPlainValue(), res,
                                                        interproceduralCFG().getMethodOf(callSite), abs);
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        // Do not pass base if tainted
                        // CallFlow passes this into the callee
                        // unless the callee is native and can not be visited
                        if (invExpr instanceof InstanceInvokeExpr
                                && aliasing.mayAlias(((InstanceInvokeExpr) invExpr).getBase(), source.getAccessPath().getPlainValue())
                                && !callee.isNative())
                            return res;

                        // Do not pass over reference parameters
                        // CallFlow passes this into the callee
                        if (Arrays.stream(callArgs).anyMatch(arg -> !isPrimtiveOrStringBase(source)
                                && aliasing.mayAlias(arg, source.getAccessPath().getPlainValue())))
                            return res;

                        if (!killSource.value)
                            res.add(source);

                        setCallSite(source, res, callStmt);
                        return res;
                    }
                };
            }

            private void setCallSite(Abstraction source, Set<Abstraction> set, Stmt callStmt) {
                for (Abstraction abs : set) {
                    if (abs != source)
                        abs.setCorrespondingCallSite(callStmt);
                }
            }

            private boolean isPrimtiveOrStringBase(Abstraction abs) {
                Type t = abs.getAccessPath().getBaseType();
                return t instanceof PrimType || TypeUtils.isStringType(t);
            }
            private boolean isPrimtiveOrStringType(Type t) {
                return t instanceof PrimType || TypeUtils.isStringType(t);
            }
        };
    }

    public TaintPropagationResults getResults() {
        return this.results;
    }
}
