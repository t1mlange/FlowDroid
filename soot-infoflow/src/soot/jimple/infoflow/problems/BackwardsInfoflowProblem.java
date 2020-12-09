package soot.jimple.infoflow.problems;

import com.sun.xml.bind.v2.runtime.reflect.opt.Const;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.flowfunc.KillAll;
import polyglot.ast.Assert;
import polyglot.ast.NewArray;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.AbstractInfoflow;
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
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.infoflow.util.TypeUtils;
import sun.security.util.Length;

import java.util.*;

/**
 * Class which contains the flow functions for the backwards analysis.
 * Not to be confused with the BackwardsAliasProblem, which is used for finding aliases.
 *
 * @author Tim Lange
 */
public class BackwardsInfoflowProblem extends AbstractInfoflowProblem {
    private final static boolean DEBUG_PRINT = false;

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

            /**
             * Returns the flow function that computes the flow for a normal statement,
             * i.e., a statement that is neither a call nor an exit statement.
             *
             * @param srcStmt The current statement.
             * @param destStmt The successor for which the flow is computed. This value can
             *             be used to compute a branched analysis that propagates
             */
            @Override
            public FlowFunction<Abstraction> getNormalFlowFunction(Unit srcStmt, Unit destStmt) {
                if (!(srcStmt instanceof Stmt))
                    return KillAll.v();

                final Aliasing aliasing = manager.getAliasing();
                if (aliasing == null)
                    return null;

                return new SolverNormalFlowFunction() {
                    @Override
                    public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
                        if (taintPropagationHandler != null)
                            taintPropagationHandler.notifyFlowIn(srcStmt, source, manager,
                                    TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);

                        final Abstraction newSource = !source.isAbstractionActive() && srcStmt == source.getActivationUnit()
                                ? source.getActiveCopy() : source;
                        Set<Abstraction> res = computeTargetsInternal(d1, newSource);
                        if (DEBUG_PRINT)
                            System.out.println("Normal" + "\n" + "In: " + source.toString() + "\n" + "Stmt: " + srcStmt.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n" + "---------------------------------------");

                        return notifyOutFlowHandlers(srcStmt, d1, source, res,
                                TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);
                    }

                    private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
                        Set<Abstraction> res = null;
                        ByReferenceBoolean killSource = new ByReferenceBoolean();
                        ByReferenceBoolean killAll = new ByReferenceBoolean();
                        // If we have a RuleManager, apply the rules
                        if (propagationRules != null) {
                            res = propagationRules.applyNormalFlowFunction(d1, source, (Stmt) srcStmt,
                                    (Stmt) destStmt, killSource, killAll);
                        }
                        // On killAll, we do not propagate anything and can stop here
                        if (killAll.value)
                            return null;

                        // Instanciate res in case the RuleManager did return null
                        if (res == null)
                            res = new HashSet<>();

                        // In normal flow only assignments are relevant
                        // We can stop here if it is no assigment
                        if (!(srcStmt instanceof AssignStmt))
                            return res;

                        final AssignStmt assignStmt = (AssignStmt) srcStmt;
                        final Value left = assignStmt.getLeftOp();
                        final Value right = assignStmt.getRightOp();
                        final Value[] rightVals = BaseSelector.selectBaseList(right, true);

                        // NewExpr can not produce new taints
                        if (right instanceof NewExpr)
                            return res;

                        // We throw the source away only if we
                        // created a new taint on the right side
                        boolean keepSource = true;

                        boolean aliasOverwritten = !source.isAbstractionActive()
                                && Aliasing.baseMatchesStrict(right, source) && right.getType() instanceof RefType
                                && !source.dependsOnCutAP();

                        if (aliasOverwritten)
                            return res;

                        // We only need to find one taint for tainting the
                        // left side so we can have those vars out of the loop
                        boolean addLeftValue = false;
                        boolean cutFirstFieldLeft = false;
                        boolean createNewApLeft = false;
                        Type leftType = null;
                        for (Value rightVal : rightVals) {
                            AccessPath ap = source.getAccessPath();

                            // If we do not overwrite the left side, we also do not
                            // create a new taint out of the right side
                            // If addLeftValue is already true, we found a taint
                            // in a previous iteration
                            if (!addLeftValue) {
                                if (rightVal instanceof FieldRef) {
                                    FieldRef ref = (FieldRef) rightVal;

                                    // If our base is null, we can stop here
                                    if (ref instanceof InstanceFieldRef
                                            && ((InstanceFieldRef) ref).getBase().getType() instanceof NullType)
                                        return null;

                                    // S.x
                                    if (rightVal instanceof StaticFieldRef
                                            && getManager().getConfig().getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None) {
                                        if (ap.firstFieldMatches(((StaticFieldRef) rightVal).getField())) {
                                            addLeftValue = true;
                                        }
                                    }
                                    // o.x
                                    else if (rightVal instanceof InstanceFieldRef) {
                                        InstanceFieldRef inst = ((InstanceFieldRef) rightVal);

                                        // base object matches
                                        if (aliasing.mayAlias(inst.getBase(), ap.getPlainValue())) {
                                            // field match
                                            if (ap.firstFieldMatches(inst.getField())) {
                                                addLeftValue = true;
                                                cutFirstFieldLeft = true;
                                            }
                                            // whole object is tainted
                                            else if (ap.getFieldCount() == 0 && ap.getTaintSubFields()) {
                                                addLeftValue = true;
                                            }
                                        }
                                    }
                                } else if (aliasing.mayAlias(rightVal, ap.getPlainValue())) {
                                    if (right instanceof InstanceOfExpr) {
                                        createNewApLeft = true;
                                        leftType = BooleanType.v();
                                    } else if (right instanceof LengthExpr) {
                                        createNewApLeft = true;
                                        leftType = IntType.v();
                                    }
                                    addLeftValue = true;
                                }
                            }


                            // Handled in the ArrayPropagationRule
                            if (right instanceof LengthExpr || right instanceof ArrayRef || right instanceof NewArrayExpr)
                                break;

                            boolean addRightValue = false;
                            boolean cutRightField = false;
                            Type rightType = null;
                            if (left instanceof FieldRef) {
                                FieldRef ref = (FieldRef) left;

                                // If our base is null, we can stop here
                                if (ref instanceof InstanceFieldRef
                                        && ((InstanceFieldRef) ref).getBase().getType() instanceof NullType)
                                    return null;

                                // S.x
                                if (left instanceof StaticFieldRef
                                        && getManager().getConfig().getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None) {
                                    if (ap.firstFieldMatches(((StaticFieldRef) left).getField())) {
                                        addRightValue = true;
                                    }
                                }
                                // o.x
                                else if (left instanceof InstanceFieldRef) {
                                    InstanceFieldRef inst = ((InstanceFieldRef) left);

                                    // base object matches
                                    if (aliasing.mayAlias(inst.getBase(), ap.getPlainValue())) {
                                        // field match
                                        if (ap.firstFieldMatches(inst.getField())) {
                                            addRightValue = true;
                                            cutRightField = true;
                                        }
                                        // whole object is tainted
                                        else if (ap.getFieldCount() == 0 && ap.getTaintSubFields()) {
                                            addRightValue = true;
                                        }
                                    }
                                }
                            }
                            // A[i] = x with A's content tainted -> taint x. A stays tainted because of keepSource
                            else if (left instanceof ArrayRef && source.getAccessPath().getArrayTaintType()
                                    != AccessPath.ArrayTaintType.Length) {
                                ArrayRef arrayRef = (ArrayRef) left;
                                // If we track indices, the indice must be tainted
                                if (getManager().getConfig().getImplicitFlowMode().trackArrayAccesses()
                                        && arrayRef.getIndex() == source.getAccessPath().getPlainValue()) {
                                    addRightValue = true;
                                    rightType = ((ArrayType) arrayRef.getBase().getType()).getElementType();
                                }
                                // else just look for the base
                                else if (aliasing.mayAlias(arrayRef.getBase(), ap.getPlainValue())) {
                                    addRightValue = true;
                                    rightType = ((ArrayType) arrayRef.getBase().getType()).getElementType();
                                }
                            }
                            // Default case, e.g. $stack1 = o.get() or o = o2;
                            else if (aliasing.mayAlias(left, ap.getPlainValue())) {
                                addRightValue = true;
                                rightType = rightVal.getType();
                            }

                            if (addRightValue) {
                                // Taint was overwritten
                                keepSource = false;

                                // We do not track constants
                                if (rightVal instanceof Constant)
                                    continue;

                                AccessPath newAp = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                        rightVal, rightType, cutRightField);
                                Abstraction newAbs = source.deriveNewAbstraction(newAp, assignStmt);
                                addAbstractionIfPossible(res, newAbs, rightVal);
                            }
                        }

                        // If we don't track array indices, we do not overwrite the
                        // taint when we overwrite one value inside the array.
                        // So we should keep the source alive regardless of the right side
                        if (left instanceof ArrayRef && !getManager().getConfig().getImplicitFlowMode().trackArrayAccesses())
                            keepSource = true;

                        if (!keepSource)
                            res.remove(source);
                        if (addLeftValue) {
                            AccessPath newAp;
                            if (createNewApLeft) {
                                newAp = manager.getAccessPathFactory().createAccessPath(left, leftType, true,
                                        AccessPath.ArrayTaintType.ContentsAndLength);
                            } else {
                                newAp = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                        left, leftType, cutFirstFieldLeft);
                            }
                            Abstraction newAbs = source.deriveNewAbstraction(newAp, assignStmt);
                            addAbstractionIfPossible(res, newAbs, left);
                            if (res.contains(newAbs) && Aliasing.canHaveAliases(newAp))
                                aliasing.computeAliases(d1, assignStmt, left, res, interproceduralCFG().getMethodOf(assignStmt), newAbs);
                        }

                        return res;
                    }
                }

                        ;
            }

            private void addAbstractionIfPossible(Set<Abstraction> res, Abstraction abs, Value val) {
                if (abs == null)
                    return;

                if (val instanceof StaticFieldRef && manager.getConfig().getStaticFieldTrackingMode()
                        == InfoflowConfiguration.StaticFieldTrackingMode.ContextFlowInsensitive) {
                    manager.getGlobalTaintManager().addToGlobalTaintState(abs);
                } else {
                    res.add(abs);
                }
            }

            /**
             * Returns the flow function that computes the flow for a call statement.
             *
             * @param callStmt          The statement containing the invoke expression giving rise to
             *                          this call.
             * @param dest
             */
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

                        final Abstraction newSource = !source.isAbstractionActive() && callStmt == source.getActivationUnit()
                                ? source.getActiveCopy() : source;
                        Set<Abstraction> res = computeTargetsInternal(d1, newSource);
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

                        // Can we use the taintWrapper results?
                        // If yes, this is done in CallToReturnFlowFunction
//                        if (taintWrapper != null && taintWrapper.isExclusive(stmt, source))
//                            return null;

                        // not used static fields do not need to be propagated
                        if (manager.getConfig().getStaticFieldTrackingMode()
                                != InfoflowConfiguration.StaticFieldTrackingMode.None
                                && source.getAccessPath().isStaticFieldRef()) {
                            // static fields first get pushed onto the stack before used,
                            // so we check for a read on the base class
                            if (!interproceduralCFG().isStaticFieldUsed(dest, source.getAccessPath().getFirstField()))
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
                                            if (!manager.getTypeUtils().checkCast(source.getAccessPath(), retVal.getType()))
                                                continue;

                                            AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(
                                                    source.getAccessPath(), returnStmt.getOp(), returnStmt.getOp().getType(), false);
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
                        if (isExecutorExecute && ie != null && aliasing.mayAlias(ie.getArg(0), source.getAccessPath().getPlainValue())) {
                            AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                    thisLocal);
                            Abstraction abs = source.deriveNewAbstraction(ap, stmt);
                            if (abs != null)
                                res.add(abs);
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

            /**
             * Returns the flow function that computes the flow for a an exit from a
             * method. An exit can be a return or an exceptional exit.
             *
             * @param callSite     One of all the call sites in the program that called the
             *                     method from which the exitStmt is actually returning. This
             *                     information can be exploited to compute a value that depends on
             *                     information from before the call.
             *                     <b>Note:</b> This value might be <code>null</code> if
             *                     using a tabulation problem with {@link IFDSTabulationProblem#followReturnsPastSeeds()}
             *                     returning <code>true</code> in a situation where the call graph
             *                     does not contain a caller for the method that is returned from.
             * @param calleeMethod The method from which exitStmt returns.
             * @param exitStmt     The statement exiting the method, typically a return or throw
             *                     statement.
             * @param returnSite   One of the successor statements of the callSite. There may be
             *                     multiple successors in case of possible exceptional flow. This
             *                     method will be called for each such successor.
             *                     <b>Note:</b> This value might be <code>null</code> if
             *                     using a tabulation problem with {@link IFDSTabulationProblem#followReturnsPastSeeds()}
             *                     returning <code>true</code> in a situation where the call graph
             *                     does not contain a caller for the method that is returned from.
             * @return
             */
            @Override
            public FlowFunction<Abstraction> getReturnFlowFunction(Unit callSite, SootMethod callee, Unit
                    exitStmt, Unit returnSite) {
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
//                final  ReturnStmt returnStmt = (exitStmt instanceof ReturnStmt) ? (ReturnStmt) exitStmt : null;

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

                        final Abstraction newSource = !source.isAbstractionActive() && callSite == source.getActivationUnit()
                                ? source.getActiveCopy() : source;
                        Set<Abstraction> res = computeTargetsInternal(newSource, calleeD1, callerD1s);
                        if (DEBUG_PRINT)
                            System.out.println("Return" + "\n" + "In: " + source.toString() + "\n" + "Stmt: " + stmt.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n" + "---------------------------------------");
                        return notifyOutFlowHandlers(exitStmt, calleeD1, source, res,
                                TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction);
                    }

                    private Set<Abstraction> computeTargetsInternal(Abstraction source, Abstraction calleeD1, Collection<Abstraction> callerD1s) {
                        if (manager.getConfig().getStopAfterFirstFlow() && !results.isEmpty())
                            return null;

                        Set<Abstraction> res = null;
                        ByReferenceBoolean killAll = new ByReferenceBoolean();
                        if (propagationRules != null)
                            res = propagationRules.applyReturnFlowFunction(callerD1s, source,
                                    (Stmt) exitStmt, (Stmt) returnSite, (Stmt) callSite, killAll);
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

                                // TODO: understand why second condition
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
                        if (isExecutorExecute && ie != null && aliasing.mayAlias(ie.getArg(0), source.getAccessPath().getPlainValue())) {
                            AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                    thisLocal);
                            Abstraction abs = source.deriveNewAbstraction(ap, stmt);
                            if (abs != null)
                                res.add(abs);
                        } else if (ie != null) {
                            for (int i = 0; i < callee.getParameterCount(); i++) {
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
                                Abstraction abs = source.deriveNewAbstraction(ap, (Stmt) exitStmt);
                                if (abs != null)
                                    res.add(abs);
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

                final boolean isSink = manager.getSourceSinkManager() != null
                        && manager.getSourceSinkManager().getSinkInfo(callStmt, manager, null) != null;
                final boolean isSource = manager.getSourceSinkManager() != null
                        && manager.getSourceSinkManager().getSourceInfo(callStmt, manager) != null;

                return new SolverCallToReturnFlowFunction() {
                    @Override
                    public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
                        // Notify the handler if we have one
                        if (taintPropagationHandler != null)
                            taintPropagationHandler.notifyFlowIn(callSite, source, manager,
                                    TaintPropagationHandler.FlowFunctionType.CallToReturnFlowFunction);

                        final Abstraction newSource = !source.isAbstractionActive() && callSite == source.getActivationUnit()
                                ? source.getActiveCopy() : source;
                        Set<Abstraction> res = computeTargetsInternal(d1, newSource);
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
                        // the value isn't used inside the method.
                        // Otherwise CallFlowFunction already does the job.
                        if (manager.getConfig().getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
                                && source.getAccessPath().isStaticFieldRef()
                                && interproceduralCFG().isStaticFieldUsed(callee, source.getAccessPath().getFirstField()))
                            return res;

                        if (callee.isNative() && ncHandler != null) {
                            for (Value arg : callArgs) {
                                if (aliasing.mayAlias(arg, source.getAccessPath().getPlainValue())) {
                                    Set<Abstraction> nativeAbs = ncHandler.getTaintedValues(callStmt, source, callArgs);
                                    if (nativeAbs != null)
                                        res.addAll(nativeAbs);

                                    break;
                                }
                            }
                        }

                        // Do not pass base if tainted
                        // CallFlow passes this into the callee
                        if (invExpr instanceof InstanceInvokeExpr
                                && aliasing.mayAlias(((InstanceInvokeExpr) invExpr).getBase(), source.getAccessPath().getPlainValue()))
                            return res;

                        // Do not pass over reference parameters
                        // CallFlow passes this into the callee
                        if (Arrays.stream(callArgs).anyMatch(arg -> !isPrimtiveOrStringBase(source) && aliasing.mayAlias(arg, source.getAccessPath().getPlainValue())))
                            return res;

                        if (!killSource.value)
                            res.add(source);

                        setCallSite(source, res, callStmt);
                        return res;
                    }
                };
            }

            private void setCallSite(Abstraction source, Set<Abstraction> set, Stmt callSite) {
                for (Abstraction abs : set) {
                    if (abs != source)
                        abs.setCorrespondingCallSite(callSite);
                }
            }

            private boolean isPrimtiveOrStringBase(Abstraction abs) {
                Type t = abs.getAccessPath().getBaseType();
                return t instanceof PrimType || TypeUtils.isStringType(t);
            }
        }

                ;
    }


    public TaintPropagationResults getResults() {
        return this.results;
    }
}
