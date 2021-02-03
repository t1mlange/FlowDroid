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
 * Not to be confused with the AliasProblem, which is used for finding aliases.
 *
 * @author Tim Lange
 */
public class BackwardsInfoflowProblem extends AbstractInfoflowProblem {
    private final static boolean DEBUG_PRINT = false;
    private final static boolean ONLY_CALLS = false;

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
            private boolean isCircularTypeMatch(Value val, Abstraction source) {
                if (!(val instanceof InstanceFieldRef))
                    return false;
                InstanceFieldRef ref = (InstanceFieldRef) val;
                return ref.getBase().getType() == ref.getField().getType() && ref.getBase() == source.getAccessPath().getPlainValue();
            }

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
                        boolean keepSource = false;
                        // Statements such as c = a + b with the taint c can produce multiple taints because we can not
                        // decide which one originated from a source at this point.
                        for (Value rightVal : rightVals) {
                            boolean addLeftValue = false;
                            boolean cutFirstFieldLeft = false;
                            boolean createNewVal = false;
                            Type leftType = null;

                            if (rightVal instanceof StaticFieldRef) {
                                StaticFieldRef staticRef = (StaticFieldRef) rightVal;

                                AccessPath mappedAp = aliasing.mayAlias(ap, staticRef);
                                if (manager.getConfig().getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
                                        && mappedAp != null) {
                                    addLeftValue = true;
                                    cutFirstFieldLeft = true;
                                    ap = mappedAp;
                                }
                            } else if (rightVal instanceof InstanceFieldRef) {
                                InstanceFieldRef instRef = (InstanceFieldRef) rightVal;

                                // Kill the taint if o = null
                                if (instRef.getBase().getType() instanceof NullType)
                                    return null;

                                AccessPath mappedAp = aliasing.mayAlias(ap, instRef);
                                // field ref match
                                if (mappedAp != null) {
                                    addLeftValue = true;
                                    // $stack1 = o.x with t=o.x -> T={$stack1}.
                                    cutFirstFieldLeft =  (mappedAp.getFieldCount() > 0
                                            && mappedAp.getFirstField() == instRef.getField());
                                    // We can't really get more precise typewise
//                                    leftType = leftVal.getType();
                                    ap = mappedAp;
                                }
                                // whole object tainted
                                else if (aliasing.mayAlias(instRef.getBase(), sourceBase)
                                        && ap.getTaintSubFields() && ap.getFieldCount() == 0) {
                                    // $stack1 = o.x with t=o.* -> T={$stack1}.
                                    addLeftValue = true;
//                                    leftType = leftVal.getType();
                                }
//                                    } else if ((isCircularTypeMatch(rightVal, source) ||source.dependsOnCutAP())
//                                            && !(leftVal.getType() instanceof PrimType)) {
//                                        addLeftValue = true;
//                                        cutFirstFieldLeft = true;
//                                    }
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
                                    newAp = manager.getAccessPathFactory().copyWithNewValue(ap,
                                            leftVal, leftType, cutFirstFieldLeft);
                                Abstraction newAbs = source.deriveNewAbstraction(newAp, assignStmt);


                                if (newAbs != null) {
                                    if (aliasing.canHaveAliasesRightSide(assignStmt, leftVal, newAbs)) {
                                        aliasing.computeAliases(d1, assignStmt, leftVal, Collections.singleton(newAbs),
                                                interproceduralCFG().getMethodOf(assignStmt), newAbs);
                                    }
                                }
                            }

                            boolean addRightValue = false;
                            boolean cutFirstField = false;
                            Type rightType = null;

                            // S.x
                            if (leftVal instanceof StaticFieldRef) {
                                StaticFieldRef staticRef = (StaticFieldRef) leftVal;

                                AccessPath mappedAp = aliasing.mayAlias(ap, staticRef);
                                // If we do not track statics, just skip this rightVal
                                if (getManager().getConfig().getStaticFieldTrackingMode()
                                        != InfoflowConfiguration.StaticFieldTrackingMode.None
                                        && mappedAp != null) {
                                    addRightValue = true;
                                    cutFirstField = true;
                                    rightType = ap.getFirstFieldType();
                                    ap = mappedAp;
                                }
                            }
                            // o.x
                            else if (leftVal instanceof InstanceFieldRef) {
                                InstanceFieldRef instRef = ((InstanceFieldRef) leftVal);

                                // Kill the taint if o = null
                                if (instRef.getBase().getType() instanceof NullType)
                                    return null;

                                AccessPath mappedAp = aliasing.mayAlias(ap, instRef);
                                // field reference match
                                if (mappedAp != null) {
                                    addRightValue = true;
                                    // o.x = $stack1 with t=o.x -> T={$stack1}. Without it would be $stack1.x.
                                    cutFirstField = (mappedAp.getFieldCount() > 0
                                            && mappedAp.getFirstField() == instRef.getField());
                                    // If there was a path expansion (cutFirstField = false), we can not
                                    // precise the type using the left field
                                    rightType = mappedAp.getFirstFieldType();
                                    ap = mappedAp;
                                }
                                // whole object tainted
                                else if (aliasing.mayAlias(instRef.getBase(), sourceBase)
                                        && ap.getTaintSubFields() && ap.getFieldCount() == 0) {
                                    // o.x = $stack1 with t=o.* -> T={$stack1}. No cut as source has no fields.
                                    addRightValue = true;
                                    rightType = instRef.getField().getType();
                                    // Because the whole object is tainted, we can not kill our source
                                    // as we do not know in which field the tainted value lies.
                                    keepSource = true;
                                }
//                                    else if ((isCircularTypeMatch(leftVal, source) || source.dependsOnCutAP())) {
//                                        addRightValue = true;
//                                        cutFirstField = true;
//                                    }
//                                }
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
                                    // We don't track indices => we don't know if the tainted value was at this index
                                    keepSource = true;
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
                                addRightValue = !(rightOp instanceof LengthExpr || rightOp instanceof ArrayRef || rightOp instanceof NewArrayExpr);
                            }

                            if (addRightValue) {
                                if (!keepSource)
                                    res.remove(source);

                                if (rightVal instanceof Constant)
                                    continue;

                                // NewExpr's can not be tainted
                                // so we can stop here
                                if (rightOp instanceof AnyNewExpr)
                                    continue;

                                AccessPath newAp = manager.getAccessPathFactory().copyWithNewValue(ap,
                                        rightVal, rightType, cutFirstField);
                                Abstraction newAbs = source.deriveNewAbstraction(newAp, assignStmt);
                                if (newAbs != null) {
                                    if (rightVal instanceof StaticFieldRef && manager.getConfig().getStaticFieldTrackingMode()
                                            == InfoflowConfiguration.StaticFieldTrackingMode.ContextFlowInsensitive)
                                        manager.getGlobalTaintManager().addToGlobalTaintState(newAbs);
                                    else {
                                        res.add(newAbs);

                                        if (isPrimitiveOrStringBase(source)) {
                                            newAbs.setTurnUnit(srcUnit);
                                        } else if (leftVal instanceof FieldRef
                                                && isPrimtiveOrStringType(((FieldRef) leftVal).getField().getType())) {
                                            newAbs.setTurnUnit(srcUnit);
                                        } else {
                                            if (aliasing.canHaveAliasesRightSide(assignStmt, rightVal, newAbs)) {
                                                aliasing.computeAliases(d1, assignStmt, rightVal, res,
                                                        interproceduralCFG().getMethodOf(assignStmt), newAbs);
                                            }
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
                                                if (isPrimitiveOrStringBase(abs))
                                                    abs.setTurnUnit(stmt);
                                                else
                                                    abs.setAliasingFlag((Stmt) callStmt);


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
                                // second condition prevents mapping o if it is also a parameter
                                if (isReflectiveCallSite
                                        || instanceInvokeExpr.getArgs().stream().noneMatch(arg -> arg == sourceBase)) {
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
                                if (isPrimitiveOrStringBase(source))
                                    continue;
                                if (!source.getAccessPath().getTaintSubFields())
                                    continue;

                                // If the variable was overwritten
                                // somewehere in the callee, we assume
                                // it to overwritten on all paths (yeah,
                                // I know ...) Otherwise, we need SSA
                                // or lots of bookkeeping to avoid FPs
                                // (BytecodeTests.flowSensitivityTest1).
                                if (interproceduralCFG().methodWritesValue(dest, paramLocals[i]))
                                    continue;

//                                if (callStmt instanceof AssignStmt) {
//                                    AssignStmt assignStmt = (AssignStmt) callStmt;
//                                    Value leftOp = assignStmt.getLeftOp();
//                                    if (aliasing.mayAlias(ie.getArg(i), leftOp))
//                                        continue;
//                                }

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
                                    if (abs != null) {
                                        res.add(abs);
                                    }
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
                                    Abstraction abs = source.deriveNewAbstraction(ap, (Stmt) exitStmt);
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
                                    res.add(abs);
                                }
                            }
                        } else if (ie != null) {
                            for (int i = 0; i < callee.getParameterCount(); i++) {
                                if (!aliasing.mayAlias(source.getAccessPath().getPlainValue(), paramLocals[i]))
                                    continue;

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
                                Abstraction abs = source.deriveNewAbstraction(ap, exitStmt);
                                if (abs != null) {
                                    res.add(abs);

//                                    if (aliasing.canHaveAliasesRightSide(callStmt, originalCallArg, abs)) {
//                                        // see HeapTests#testAliases
//                                        // we are unable to trigger
//                                        for (Abstraction d1 : callerD1s)
//                                            for (Unit succ : manager.getICFG().getSuccsOf(callSite))
//                                                aliasing.computeAliases(d1, (Stmt) succ, originalCallArg, res,
//                                                    interproceduralCFG().getMethodOf(callSite), abs);
//                                    }

                                    if (aliasing.canHaveAliasesRightSide(callStmt, originalCallArg, abs)) {
                                        // see HeapTests#testAliases
                                        // If two arguments are the same, we created an alias
                                        // so we fully revisit the callSite using aliasing
                                        for (int argIndex = 0; argIndex < i; argIndex++) {
                                            if (originalCallArg == ie.getArg(argIndex)) {
                                                for (Abstraction d1 : callerD1s)
                                                    for (Unit succ : manager.getICFG().getSuccsOf(callSite))
                                                        aliasing.computeAliases(d1, (Stmt) succ, originalCallArg, res,
                                                                interproceduralCFG().getMethodOf(callSite), abs);
                                                break;
                                            }
                                        }

                                        // If the turn unit is inside the callee, no need
                                        // for aliasing on the returned value
                                        if (callee == manager.getICFG().getMethodOf(abs.getTurnUnit()))
                                            abs.setAliasingFlag(null);

                                        // Trigger alias on returned value
                                        // See HeapTests#tripleAlias
                                        if (abs.getAliasingFlag() == callStmt && callStmt instanceof AssignStmt
                                                && aliasing.canHaveAliasesRightSide(callStmt, originalCallArg, abs)) {
                                            abs.setAliasingFlag(null);

                                            // Here we can skip the whole call site as we are searching for the
                                            // returned value
                                            for (Abstraction callerD1 : callerD1s)
                                                for (Unit pred : manager.getICFG().getPredsOf(callSite))
                                                    aliasing.computeAliases(callerD1, (Stmt) pred, originalCallArg, res,
                                                            interproceduralCFG().getMethodOf(callSite), abs);
                                        }
                                    } else {
                                        // just to be sure everything is cleaned up
                                        if (abs.getAliasingFlag() == callStmt)
                                            abs.setAliasingFlag(null);
                                    }


//                                        if (!isPrimtiveOrStringType(leftOp.getType())
//                                                && manager.getTypeUtils().checkCast(abs.getAccessPath().getBaseType(), leftOp.getType())
//                                                && aliasing.canHaveAliasesRightSide((Stmt) callSite, originalCallArg, source)) {
//
//                                            // trigger aliasing if param gets returned
//                                            for (Unit unit : callee.getActiveBody().getUnits()) {
//                                                if (unit instanceof ReturnStmt && !isExceptionHandler(unit)) {
//                                                    Value retVal = ((ReturnStmt) unit).getOp();
//                                                    if (aliasing.mayAlias(retVal, paramLocals[i])) {
//                                                        for (Abstraction callerD1 : callerD1s)
//                                                            aliasing.computeAliases(callerD1, (Stmt) callSite, originalCallArg, res, callee, abs);
//                                                    }
//                                                }
//                                            }
//                                        }
//                                    }
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

                        if (source.toString().contains("r2") && callStmt.toString().contains("getChars"))
                            d1=d1;

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
                            if (source != zeroValue)
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
                        if (Arrays.stream(callArgs).anyMatch(arg -> !isPrimitiveOrStringBase(source)
                                && aliasing.mayAlias(arg, source.getAccessPath().getPlainValue())))
                            return res;

                        if (!killSource.value && source != zeroValue)
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

            private boolean isPrimitiveOrStringBase(Abstraction abs) {
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
