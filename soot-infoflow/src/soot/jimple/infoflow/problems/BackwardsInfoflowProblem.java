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

import java.util.*;

/**
 * Class which contains the flow functions for the backwards analysis.
 * Not to be confused with the BackwardsAliasProblem, which is used for finding aliases.
 *
 * @author Tim Lange
 */
public class BackwardsInfoflowProblem extends AbstractInfoflowProblem {
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

                return new SolverNormalFlowFunction() {
                    @Override
                    public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
                        if (taintPropagationHandler != null)
                            taintPropagationHandler.notifyFlowIn(srcStmt, source, manager,
                                    TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);

                        Set<Abstraction> res = computeTargetsInternal(d1, source);

                        return notifyOutFlowHandlers(srcStmt, d1, source, res,
                                TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);
                    }

                    private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
                        // TODO: activation needed?

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
                            return Collections.emptySet();

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

                        // If left side is not tainted, no normal flow rules apply
                        // and we propagate the taint over the statement
                        if (!Aliasing.baseMatches(left, source)) {
                            res.add(source);
                            return res;
                        }

                        // Handled in the ArrayPropagationRule (TODO: really?)
                        // As we are backwards, our source is thrown thrown away
                        // because it was overwritten with the rightValue
                        if (right instanceof LengthExpr)
                            return Collections.emptySet();

                        // We do not know which right value is responsible for the taint.
                        // Being conservative, we taint all rhs variables
                        for (Value rightVal : rightVals) {
                            // null refrences kill the taint
                            if (rightVal instanceof InstanceFieldRef && rightVal.getType() instanceof NullType)
                                continue; // TODO: or break?

                            // We do not track constants
                            if (rightVal instanceof Constant)
                                continue;

                            // TODO: cutFirstField?
                            Abstraction newAbs;
                            // x = y.g;
                            if (source.getAccessPath().isEmpty()) {
                                newAbs = source.deriveNewAbstraction(manager.getAccessPathFactory()
                                        .createAccessPath(rightVal, true), assignStmt);
                            }
                            // x.f = y.g
                            else {
                                AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                        rightVal);
                                newAbs = source.deriveNewAbstraction(ap, assignStmt);
                            }

                            // Special cases for static taint tracking options
                            if (rightVal instanceof StaticFieldRef && manager.getConfig().getStaticFieldTrackingMode()
                                    == InfoflowConfiguration.StaticFieldTrackingMode.ContextFlowInsensitive)
                                manager.getGlobalTaintManager().addToGlobalTaintState(newAbs);
                            else if (rightVal instanceof StaticFieldRef && manager.getConfig()
                                    .getStaticFieldTrackingMode() == InfoflowConfiguration.StaticFieldTrackingMode.None)
                                return res;
                            else
                                res.add(newAbs);
                        }

                        return res;
                    }
                };
            }

            /**
             * Returns the flow function that computes the flow for a call statement.
             *
             * @param callStmt          The statement containing the invoke expression giving rise to
             *                          this call.
             * @param dest
             */
            @Override
            public FlowFunction<Abstraction> getCallFlowFunction(final Unit callStmt, final SootMethod dest) {
                if (!dest.isConcrete()) {
                    logger.debug("Call skipped because target has no body: {} -> {}", callStmt, dest);
                    return KillAll.v();
                }

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
                            return Collections.emptySet();

                        // Notify the handler if we have one
                        if (taintPropagationHandler != null)
                            taintPropagationHandler.notifyFlowIn(stmt, source, manager,
                                    TaintPropagationHandler.FlowFunctionType.CallFlowFunction);

                        Set<Abstraction> res = computeTargetsInternal(d1,source);

                        return notifyOutFlowHandlers(stmt, d1, source, res,
                                TaintPropagationHandler.FlowFunctionType.CallFlowFunction);
                    }
                    private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
                        // Respect user settings
                        if (manager.getConfig().getStopAfterFirstFlow() && !results.isEmpty())
                            return Collections.emptySet();
                        if (!manager.getConfig().getInspectSources() && isSource)
                            return Collections.emptySet();
                        if (!manager.getConfig().getInspectSinks() && isSink)
                            return Collections.emptySet();
                        if (manager.getConfig().getStaticFieldTrackingMode() ==
                                InfoflowConfiguration.StaticFieldTrackingMode.None && dest.isStaticInitializer())
                            return Collections.emptySet();

                        // Do not propagate into Soot library classes if that
                        // optimization is enabled
                        if (isExcluded(dest))
                            return Collections.emptySet();

                        // Can we use the taintWrapper results?
                        if (taintWrapper != null && taintWrapper.isExclusive(stmt, source))
                            return Collections.emptySet();

                        // We might need to activate the abstraction TODO: Do we?
//                        if (!source.isAbstractionActive() && source.getActivationUnit() == callStmt)
//                            source = source.getActiveCopy();

                        // not read static fields do not need to be propagated
//                        if (manager.getConfig().getStaticFieldTrackingMode()
//                                != InfoflowConfiguration.StaticFieldTrackingMode.None
//                                && source.getAccessPath().isStaticFieldRef()) {
//                            if (!interproceduralCFG().isStaticFieldRead(dest, source.getAccessPath().getFirstField()))
//                                return null;
//                        }

                        Set<Abstraction> res = null;
                        ByReferenceBoolean killAll = new ByReferenceBoolean();
                        if (propagationRules != null)
                             res = propagationRules.applyCallFlowFunction(d1, source, stmt, dest, killAll);
                        if (killAll.value)
                            return Collections.emptySet();

                        // Instanciate in case RuleManager did not produce an object
                        if (res == null)
                            res = new HashSet<>();

                        // x = o.m(a1, ..., an)
                        if (callStmt instanceof AssignStmt) {
                            AssignStmt assignStmt = (AssignStmt) callStmt;
                            Value left = assignStmt.getLeftOp();

                            // we only taint the return statement(s) if x is tainted
                            if (left == source.getAccessPath().getPlainValue()) {
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
                                                    source.getAccessPath(), returnStmt.getOp(), null, false);
                                            Abstraction abs = source.deriveNewAbstraction(ap, stmt);
                                            if (abs != null)
                                                res.add(abs);
                                        }
                                    }
                                }
                            }
                        }

                        // static fields access path stay the same :)
                        // maybe handled by a rule
//                        if (manager.getConfig().getStaticFieldTrackingMode() !=
//                                InfoflowConfiguration.StaticFieldTrackingMode.None
//                            && source.getAccessPath().isStaticFieldRef()) {
//                            Abstraction abs = source.deriveNewAbstraction(source.getAccessPath(), stmt);
//                            if (abs != null)
//                                res.add(abs);
//                        }

                        // o.m(a1, ..., an)
                        // map o.f to this.f
                        if (!isExecutorExecute && !source.getAccessPath().isStaticFieldRef() && !dest.isStatic()) {
                            InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
                            Value callBase = isReflectiveCallSite ?
                                    instanceInvokeExpr.getArg(0) : instanceInvokeExpr.getBase();

                            Value sourceBase = source.getAccessPath().getPlainValue();
                            if (callBase == sourceBase && manager.getTypeUtils()
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
                        if (isExecutorExecute && ie != null && ie.getArg(0) == source.getAccessPath().getPlainValue()) {
                            AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                    thisLocal);
                            Abstraction abs = source.deriveNewAbstraction(ap, stmt);
                            if (abs != null)
                                res.add(abs);
                        } else if (ie != null && dest.getParameterCount() > 0) {
//                            List<Value> args = ie.getArgs();
//                            if (isReflectiveCallSite)
//                                args.remove(0);
                            for (int i = isReflectiveCallSite ? 1 : 0; i < ie.getArgCount(); i++) {
                                if (ie.getArg(i) != source.getAccessPath().getPlainValue())
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
            public FlowFunction<Abstraction> getReturnFlowFunction(Unit callSite, SootMethod callee, Unit exitStmt, Unit returnSite) {
                if (callSite != null && !(callSite instanceof Stmt))
                    return KillAll.v();

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
                            return Collections.emptySet();
                        if (callSite == null)
                            return Collections.emptySet();

                        if (taintPropagationHandler != null)
                            taintPropagationHandler.notifyFlowIn(stmt, source, manager,
                                    TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction);

                        Set<Abstraction> res = computeTargetsInternal(source, calleeD1, callerD1s);

                        return notifyOutFlowHandlers(exitStmt, calleeD1, source, res,
                                TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction);
                    }

                    private Set<Abstraction> computeTargetsInternal(Abstraction source, Abstraction calleeD1, Collection<Abstraction> callerD1s) {
                        // TODO: understand
//                        if (manager.getConfig().getStaticFieldTrackingMode() !=
//                                InfoflowConfiguration.StaticFieldTrackingMode.None
//                            && source.getAccessPath().isStaticFieldRef()) {
//                            registerActivationCallSite(callSite, callee, source);
//                            return Collections.singleton(source);
//                        }
                        Set<Abstraction> res = null;
                        ByReferenceBoolean killAll = new ByReferenceBoolean();
                        if (propagationRules != null)
                            res = propagationRules.applyReturnFlowFunction(callerD1s, source,
                                (Stmt) exitStmt, (Stmt) returnSite, (Stmt) callSite, killAll);
                        if (killAll.value)
                            return Collections.emptySet();

                        if (res == null)
                            res = new HashSet<>();


                        // o.m(a1, ..., an)
                        // map o.f to this.f
                        if (!isExecutorExecute && !callee.isStatic()) {
                            Value sourceBase = source.getAccessPath().getPlainValue();
                            if (thisLocal == sourceBase && manager.getTypeUtils()
                                    .hasCompatibleTypesForCall(source.getAccessPath(), callee.getDeclaringClass())) {

                                InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
                                Value callBase = isReflectiveCallSite ?
                                        instanceInvokeExpr.getArg(0) : instanceInvokeExpr.getBase();


                                // TODO: understand why second condition
                                if (isReflectiveCallSite ||
                                        instanceInvokeExpr.getArgs().stream().noneMatch(arg -> arg == sourceBase)) {
                                    AccessPath ap = manager.getAccessPathFactory()
                                            .copyWithNewValue(source.getAccessPath(), callBase, isReflectiveCallSite ? null
                                                            : source.getAccessPath().getBaseType(),false);
                                    Abstraction abs = source.deriveNewAbstraction(ap, (Stmt) exitStmt);
                                    if (abs != null) {
                                        res.add(abs);
                                        registerActivationCallSite(callSite, callee, abs);
                                    }
                                }
                            }
                        }

                        // map arguments to parameter
                        if (isExecutorExecute && ie != null && ie.getArg(0) == source.getAccessPath().getPlainValue()) {
                            AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                    thisLocal);
                            Abstraction abs = source.deriveNewAbstraction(ap, stmt);
                            if (abs != null)
                                res.add(abs);
                        } else if (ie != null && callee.getParameterCount() > 0) {
//                            List<Value> args = ie.getArgs();
//                            if (isReflectiveCallSite)
//                                args.remove(0);
                            for (int i = 0; i < paramLocals.length; i++) {
                                Value originalCallArg = ie.getArg(isReflectiveCallSite ? 1 : i);

                                if (!AccessPath.canContainValue(originalCallArg))
                                    continue;
                                if (!isReflectiveCallSite && !manager.getTypeUtils()
                                        .checkCast(source.getAccessPath(), originalCallArg.getType()))
                                    continue;
                                // If param is overwritten in the method, the parameter taint is killed
                                // and the argument will not be tainted
                                if (interproceduralCFG().methodWritesValue(callee, paramLocals[i]))
                                    continue;

                                AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                        originalCallArg,
                                        isReflectiveCallSite ? null : source.getAccessPath().getBaseType(), false);
                                Abstraction abs = source.deriveNewAbstraction(ap, (Stmt) exitStmt);
                                if (abs != null) {
                                    res.add(abs);
                                    registerActivationCallSite(callSite, callee, abs);
                                }
                            }
                        }

                        return res;
                    }
                };
            }

            /**
             * Returns the flow function that computes the flow from a call site to a
             * successor statement just after the call. There may be multiple successors
             * in case of exceptional control flow. In this case this method will be
             * called for every such successor. Typically, one will propagate into a
             * method call, using {@link #getCallFlowFunction(Object, Object)}, only
             * such information that actually concerns the callee method. All other
             * information, e.g. information that cannot be modified by the call, is
             * passed along this call-return edge.
             *
             * @param callSite   The statement containing the invoke expression giving rise to
             *                   this call.
             * @param returnSite The return site to which the information is propagated. For
             *                   exceptional flow, this may actually be the start of an
             */
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

                    /**
                     * Computes the abstractions at the return site
                     *
                     * @param d1 The abstraction at the beginning of the caller, i.e. the
                     *           context in which the method call is made
                     * @param d2 The abstraction at the call site
                     * @return The set of abstractions at the first node inside the callee
                     */
                    @Override
                    public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
                        if (source == getZeroValue())
                            return Collections.emptySet();

                        // Notify the handler if we have one
                        if (taintPropagationHandler != null)
                            taintPropagationHandler.notifyFlowIn(callSite, source, manager,
                                    TaintPropagationHandler.FlowFunctionType.CallToReturnFlowFunction);

                        Set<Abstraction> res = computeTargetsInternal(d1, source);

                        return notifyOutFlowHandlers(callSite, d1, source, res,
                                TaintPropagationHandler.FlowFunctionType.CallToReturnFlowFunction);
                    }

                    private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
//                        final Abstraction newSource;
//                        if (!source.isAbstractionActive() && (callStmt == source.getActivationUnit()
//                                || isCallSiteActivatingTaint(callStmt, source.getActivationUnit())))
//                            newSource = source.getActiveCopy();
//                        else
//                            newSource = source;

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
                            return Collections.emptySet(); // TODO: null or emptySet difference?
                        boolean passOn = !killSource.value;

                        if (res == null)
                            res = new HashSet<>();

                        if (source.getAccessPath().isStaticFieldRef())
                            passOn = false;

//                        // Static fields are always in scope of the callee
//                        if (newSource.getAccessPath().isStaticFieldRef())
//                            passOn = false;
//
//                        // we only can remove the taint if we step into the
//                        // call/return edges
//                        // otherwise we will loose taint - see
//                        // ArrayTests/arrayCopyTest
//                        if (passOn && invExpr instanceof InstanceInvokeExpr
//                                && (manager.getConfig().getInspectSources() || !isSource)
//                                && (manager.getConfig().getInspectSinks() || !isSink)
//                                && newSource.getAccessPath().isInstanceFieldRef() && (hasValidCallees
//                                || (taintWrapper != null && taintWrapper.isExclusive(iCallStmt, newSource)))) {
//
//                        }
//
//                        // If the callee does not read the given value, we also
//                        // need to pass it on since we do not propagate it into
//                        // the callee.
//                        if (source.getAccessPath().isStaticFieldRef()) {
//                            if (!interproceduralCFG().isStaticFieldUsed(callee, source.getAccessPath().getFirstField()))
//                                passOn = true;
//                        }
//
//                        if (callee.isNative()) {
//                            // TODO
//                        }

//                        for (Abstraction abs : res)
//                            if (abs != newSource)
//                                abs.setCorrespondingCallSite(callStmt);

                        return res;
                    }
                };
            }
        };
    }

    public TaintPropagationResults getResults() {
        return this.results;
    }
}
