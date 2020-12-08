package soot.jimple.infoflow.problems;

import com.sun.xml.bind.v2.runtime.reflect.opt.Const;
import fj.Hash;
import heros.FlowFunction;
import heros.FlowFunctions;
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

import java.util.*;

/**
 * Class which contains the alias analysis for the backwards analysis.
 *
 * @author Tim Lange
 */
public class ForwardsAliasProblem extends AbstractInfoflowProblem {
    public ForwardsAliasProblem(InfoflowManager manager) {
        super(manager);
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
                        if (source == getZeroValue())
                            return null;

                        if (taintPropagationHandler != null)
                            taintPropagationHandler.notifyFlowIn(srcStmt, source, manager,
                                    TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);

                        Set<Abstraction> res = computeTargetsInternal(d1, source);
                        // System.out.println("Normal" + "\n" + "In: " + source.toString() + "\n" + "Stmt: " + srcStmt.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n" + "---------------------------------------");

                        return notifyOutFlowHandlers(srcStmt, d1, source, res,
                                TaintPropagationHandler.FlowFunctionType.NormalFlowFunction);
                    }

                    private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
                        HashSet<Abstraction> res = new HashSet<>();
                        // by default the source taint is kept alive

                        if (srcStmt instanceof IdentityStmt) {
                            res.add(source);
                            return res;
                        }
                        if (!(srcStmt instanceof AssignStmt))
                            return res;

                        final AssignStmt assignStmt = (AssignStmt) srcStmt;
                        final Value left = assignStmt.getLeftOp();
                        final Value right = assignStmt.getRightOp();
                        final Value rightVal = BaseSelector.selectBase(right, false);

                        // BinopExpr can only contain PrimTypes
                        // and PrimTypes aren't tracked
                        if (right instanceof BinopExpr)
                            return res;

                        // If right side is not tainted
                        // we do not produce new taints
                        if (!Aliasing.baseMatches(left, source))
                            res.add(source);
                        else {
                            for (Unit u : interproceduralCFG().getSuccsOf(assignStmt))
                                manager.getForwardSolver().processEdge(new PathEdge<Unit, Abstraction>(d1, u, source));
                        }

                        // If left side won't be tracked anyways
                        // we can already stop here
                        if (!(left instanceof Local || left instanceof FieldRef))
                            return res;

                        boolean aliasOverwritten = Aliasing.baseMatchesStrict(rightVal, source)
                                && rightVal.getType() instanceof RefType && !source.dependsOnCutAP();

                        Abstraction newAbs = null;
                        if (!aliasOverwritten) {
                            if (rightVal instanceof InstanceFieldRef) {
                                InstanceFieldRef ref = (InstanceFieldRef) rightVal;
                                if (source.getAccessPath().isInstanceFieldRef()
                                        && ref.getBase() == source.getAccessPath().getPlainValue()
                                        && source.getAccessPath().firstFieldMatches(ref.getField())) {
                                    AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                            left, source.getAccessPath().getFirstFieldType(), true);
                                    newAbs = checkAbstraction(source.deriveNewAbstraction(ap, assignStmt));
                                }
                            } else if (manager.getConfig().getStaticFieldTrackingMode() != InfoflowConfiguration.StaticFieldTrackingMode.None
                                    && rightVal instanceof StaticFieldRef) {
                                StaticFieldRef ref = (StaticFieldRef) rightVal;
                                if (source.getAccessPath().isStaticFieldRef()
                                        && source.getAccessPath().firstFieldMatches(ref.getField())) {
                                    AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                            left, source.getAccessPath().getBaseType(), true);
                                    newAbs = checkAbstraction(source.deriveNewAbstraction(ap, assignStmt));
                                }
                            } else if (rightVal == source.getAccessPath().getPlainValue()) {
                                Type newType = source.getAccessPath().getBaseType();
                                if (left instanceof ArrayRef) {
                                    ArrayRef arrayRef = (ArrayRef) left;
                                    newType = TypeUtils.buildArrayOrAddDimension(newType, arrayRef.getType().getArrayType());
                                } else if (right instanceof ArrayRef) {
                                    newType = ((ArrayType) newType).getElementType();
                                } else {
                                    // Type check
                                    if (!manager.getTypeUtils().checkCast(source.getAccessPath(), left.getType()))
                                        return null;
                                }

                                // If the cast was realizable, we can assume that we had
                                // the type to which we cast. Do not loosen types,
                                // though.
                                if (assignStmt.getRightOp() instanceof CastExpr) {
                                    CastExpr ce = (CastExpr) assignStmt.getRightOp();
                                    if (!manager.getHierarchy().canStoreType(newType, ce.getCastType()))
                                        newType = ce.getCastType();
                                }
                                // Special type handling for certain operations
                                else if (assignStmt.getRightOp() instanceof LengthExpr) {
                                    // ignore. The length of an array is a primitive and
                                    // thus cannot have aliases
                                    return res;
                                } else if (assignStmt.getRightOp() instanceof InstanceOfExpr) {
                                    // ignore. The type check of an array returns a
                                    // boolean which is a primitive and thus cannot
                                    // have aliases
                                    return res;
                                }

                                AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
                                        left, newType, false);
                                newAbs = checkAbstraction(source.deriveNewAbstraction(ap, assignStmt));
                            }
                        }

                        if (newAbs != null) {
                            if (newAbs.getAccessPath().getLastFieldType() instanceof PrimType)
                                return res;

                            if (newAbs != source) {
                                res.add(newAbs);

                                // Inject the new alias into the forward solver
                                for (Unit u : interproceduralCFG().getSuccsOf(assignStmt))
                                    manager.getForwardSolver()
                                            .processEdge(new PathEdge<Unit, Abstraction>(d1, u, newAbs));
                            }
                        }

                        return res;
                    }
                };
            }

            /**
             * Returns the flow function that computes the flow for a call statement.
             *
             * @param callSite          The statement containing the invoke expression giving rise to
             *                          this call.
             * @param dest
             */
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

                        Set<Abstraction> res = computeTargetsInternal(d1,source);
//                        System.out.println("Call" + "\n" + "In: " + source.toString() + "\n" + "Stmt: " + stmt.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n" + "---------------------------------------");

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

                        Set<Abstraction> res = computeTargetsInternal(source, calleeD1, callerD1s);
//                        System.out.println("Return" + "\n" + "In: " + source.toString() + "\n" + "Stmt: " + stmt.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n" + "---------------------------------------");

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

                        Set<Abstraction> res = computeTargetsInternal(d1, source);
//                        System.out.println("CallToReturn" + "\n" + "In: " + source.toString() + "\n" + "Stmt: " + callStmt.toString() + "\n" + "Out: " + (res == null ? "[]" : res.toString()) + "\n" + "---------------------------------------");

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

            private boolean isPrimtiveOrStringBase(Abstraction abs) {
                Type t = abs.getAccessPath().getBaseType();
                return t instanceof PrimType || TypeUtils.isStringType(t);
            }
        };
    }
}
