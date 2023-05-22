package soot.jimple.infoflow.collections;

import heros.SynchronizedBy;
import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.data.CollectionMethod;
import soot.jimple.infoflow.collections.data.CollectionModel;
import soot.jimple.infoflow.collections.operations.ICollectionOperation;
import soot.jimple.infoflow.collections.strategies.ConstantKeyStrategy;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.solver.IFollowReturnsPastSeedsHandler;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.util.ConcurrentHashMultiMap;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.util.*;

/**
 * Taint Wrapper that models the behavior of collections w.r.t. keys or indices
 *
 * @author Tim Lange
 */
public class CollectionTaintWrapper implements ITaintPropagationWrapper {
    @SynchronizedBy("Read-only during analysis")
    private final Map<String, CollectionModel> models;

    @SynchronizedBy("Read-only during analysis")
    private final ITaintPropagationWrapper fallbackWrapper;

    @SynchronizedBy("Benign race")
    private int wrapperHits;
    @SynchronizedBy("Benign race")
    private int wrapperMisses;

    private InfoflowManager manager;

    private IContainerStrategy strategy;

    public CollectionTaintWrapper(Map<String, CollectionModel> models,
                                  ITaintPropagationWrapper fallbackWrapper) {
        this.models = models;
        this.fallbackWrapper = fallbackWrapper;
        this.wrapperHits = 0;
        this.wrapperMisses = 0;
    }

    @Override
    public void initialize(InfoflowManager manager) {
        if (fallbackWrapper != null)
            fallbackWrapper.initialize(manager);

        this.manager = manager;

        this.strategy = new ConstantKeyStrategy(manager);
        manager.getMainSolver().setFollowReturnsPastSeedsHandler(new CollectionCallbackHandler());
    }

    @Override
    public Collection<PreAnalysisHandler> getPreAnalysisHandlers() {
        if (fallbackWrapper != null)
            return fallbackWrapper.getPreAnalysisHandlers();

        return Collections.emptyList();
    }

    private Set<Abstraction> fallbackTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
        if (fallbackWrapper != null)
            return fallbackWrapper.getTaintsForMethod(stmt, d1, taintedPath);

        wrapperMisses++;
        return null;
    }

    private ContextDefinition merge(ContextDefinition a, ContextDefinition b) {
//        if (a instanceof ConstantContext && b instanceof ConstantContext) {
//            HashSet<Constant> c = new HashSet<>();
//            c.addAll(((ConstantContext) a).getConstants());
//            c.addAll(((ConstantContext) b).getConstants());
//            return new ConstantContext(c);
//        }

        return UnknownContext.v();
    }

    public Set<Abstraction> mergeAbstractions(Set<Abstraction> abs) {
        MultiMap<Abstraction, Abstraction> mm = new HashMultiMap<>();

        for (Abstraction curr : abs) {
            if (curr.getAccessPath().getFragmentCount() == 0)
                continue;

            boolean foundEq = false;
            for (Abstraction seen : mm.keySet()) {
                if (seen.equalsWithoutContext(curr)) {
                    mm.put(seen, curr);
                    foundEq = true;
                    break;
                }
            }
            if (!foundEq)
                mm.put(curr, curr);
        }

        Set<Abstraction> out = new HashSet<>();
        for (Abstraction key : mm.keySet()) {
            Map<Integer, ContextDefinition> newCtxt = new HashMap<>();
            boolean smashed = false;
            for (Abstraction eqAbs : mm.get(key)) {
                AccessPathFragment f = eqAbs.getAccessPath().getFirstFragment();
                if (!f.hasContext()) {
                    out.add(eqAbs);
                    smashed = true;
                    break;
                }
                ContextDefinition[] ctxt = f.getContext();
                for (int i = 0; i < ctxt.length; i++) {
                    newCtxt.putIfAbsent(i, ctxt[i]);
                    final int j = i;
                    newCtxt.computeIfPresent(i, (k, v) -> merge(v, ctxt[j]));
                }
            }
            if (!smashed) {
                ContextDefinition[] ctxt = new ContextDefinition[newCtxt.size()];
                for (int i = 0; i < ctxt.length; i++) {
                    ctxt[i] = newCtxt.get(i);
                }
                if (strategy.shouldSmash(ctxt))
                    ctxt = null;

                AccessPathFragment[] oldFragments = key.getAccessPath().getFragments();
                AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length];
                System.arraycopy(oldFragments, 1, fragments, 1, fragments.length - 1);
                fragments[0] = oldFragments[0].copyWithNewContext(ctxt);
                AccessPath ap = manager.getAccessPathFactory().createAccessPath(key.getAccessPath().getPlainValue(), fragments,
                        key.getAccessPath().getTaintSubFields());
                out.add(key.deriveNewAbstraction(ap, null));
            }
        }

        return out;
    }

    private static class Callback {
        Abstraction d1;
        Abstraction d2;
        Unit u;

        public Callback(Abstraction d1, Unit u, Abstraction d2) {
            this.d1 = d1;
            this.u = u;
            this.d2 = d2;
        }
    }

    private class CollectionCallbackHandler implements IFollowReturnsPastSeedsHandler {
        @Override
        public void handleFollowReturnsPastSeeds(Abstraction d1, Unit u, Abstraction d2) {
            if (u instanceof ReturnStmt) {
                Value retOp = ((ReturnStmt) u).getOp();
                if (manager.getAliasing().mayAlias(d2.getAccessPath().getPlainValue(), retOp)) {
                    SootMethod sm = manager.getICFG().getMethodOf(u);
                    Set<Callback> pairs = callbacks.get(new Pair<>(d1, sm));
                    if (pairs != null) {
                        for (Callback p : pairs) {
                            for (Unit s : manager.getICFG().getSuccsOf(p.u)) {
                                PathEdge<Unit, Abstraction> e = new PathEdge<>(p.d1, s, p.d2);
                                manager.getMainSolver().processEdge(e);
                            }
                        }
                    }
                }
            }
        }
    }

    MultiMap<Pair<Abstraction, SootMethod>, Callback> callbacks = new ConcurrentHashMultiMap<>();
    public void registerCallback(Abstraction d1, Stmt stmt, Abstraction curr, Abstraction queued, SootMethod sm) {
        callbacks.put(new Pair<>(queued, sm), new Callback(d1, stmt, curr));
    }

    @Override
    public Set<Abstraction> getTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
        if (!stmt.containsInvokeExpr())
            return fallbackTaintsForMethod(stmt, d1, taintedPath);

        CollectionMethod method = getCollectionMethodForSootMethod(stmt.getInvokeExpr().getMethod());
        if (method == null)
            return fallbackTaintsForMethod(stmt, d1, taintedPath);

        wrapperHits++;

        Set<Abstraction> outSet = new HashSet<>();
        boolean killCurrentTaint = false;
        for (ICollectionOperation op : method.operations()) {
            killCurrentTaint |= op.apply(d1, taintedPath, stmt, manager, strategy, outSet);
        }

        if (!killCurrentTaint)
            outSet.add(taintedPath);

        return outSet;
    }

    @Override
    public boolean isExclusive(Stmt stmt, Abstraction taintedPath) {
        if (supportsCallee(stmt))
            return true;

        if (fallbackWrapper != null)
            return fallbackWrapper.isExclusive(stmt, taintedPath);
        return false;
    }

    @Override
    public Set<Abstraction> getAliasesForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
        if (fallbackWrapper != null)
            return fallbackWrapper.getAliasesForMethod(stmt, d1, taintedPath);
        return null;
    }

    private CollectionMethod getCollectionMethodForSootMethod(SootMethod sm) {
        CollectionModel model = models.get(sm.getDeclaringClass().getName());
        if (model != null) {
            return model.getMethod(sm.getSubSignature());
        }
        return null;
    }

    @Override
    public boolean supportsCallee(SootMethod sm) {
        if (getCollectionMethodForSootMethod(sm) != null)
            return true;

        return fallbackWrapper != null && fallbackWrapper.supportsCallee(sm);
    }

    @Override
    public boolean supportsCallee(Stmt callSite) {
        if (callSite.containsInvokeExpr())
            return supportsCallee(callSite.getInvokeExpr().getMethod());
        return fallbackWrapper != null && fallbackWrapper.supportsCallee(callSite);
    }

    @Override
    public int getWrapperHits() {
        return this.wrapperHits + fallbackWrapper.getWrapperHits();
    }

    @Override
    public int getWrapperMisses() {
        return this.wrapperMisses + fallbackWrapper.getWrapperMisses();
    }
}
