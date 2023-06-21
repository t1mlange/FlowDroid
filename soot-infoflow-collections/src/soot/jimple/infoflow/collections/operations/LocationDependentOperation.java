package soot.jimple.infoflow.collections.operations;

import java.util.List;

import soot.SootField;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.data.ParamIndex;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

public abstract class LocationDependentOperation extends AbstractOperation {
    protected final Location[] locations;
    protected final String field;

    public LocationDependentOperation(Location[] locations, String field) {
        this.locations = locations;
        this.field = field;
    }

    public ContextDefinition[] buildContext(IContainerStrategy strategy, InstanceInvokeExpr iie, Stmt stmt) {
        ContextDefinition[] ctxt = new ContextDefinition[locations.length];
        for (int i = 0; i < ctxt.length; i++) {
            int idx = locations[i].getParamIdx();
            switch (idx) {
                case ParamIndex.ALL:
                case ParamIndex.COPY:
                    throw new RuntimeException("Unexpected location index to build context: " + idx);
                case ParamIndex.LAST_INDEX:
                    ctxt[i] = strategy.getNextPosition(iie.getBase(), stmt);
                    break;
                case ParamIndex.FIRST_INDEX:
                    ctxt[i] = strategy.getFirstPosition(iie.getBase(), stmt);
                    break;
                default:
                    if (idx < 0)
                        throw new RuntimeException("Unknown location index supplied: " + idx);
                    if (locations[i].isValueBased())
                        ctxt[i] = strategy.getIndexContext(iie.getArg(locations[i].getParamIdx()), stmt);
                    else
                        ctxt[i] = strategy.getKeyContext(iie.getArg(locations[i].getParamIdx()), stmt);
                    break;
            }
        }

        return strategy.shouldSmash(ctxt) ? null : ctxt;
    }

    protected AccessPathFragment getFragment(Abstraction abs) {
        AccessPathFragment fragment = abs.getAccessPath().getFirstFragment();
        if (fragment != null && fragment.getField().getSignature().equals(this.field))
            return fragment;
        return null;
    }

    protected Tristate matchContexts(AccessPathFragment fragment, InstanceInvokeExpr iie, Stmt stmt,
                                     IContainerStrategy strategy, List<ContextDefinition> copied) {
        return matchContexts(fragment, iie, stmt, strategy, copied, false);
    }

    protected Tristate matchContexts(AccessPathFragment fragment, InstanceInvokeExpr iie, Stmt stmt,
                                     IContainerStrategy strategy, List<ContextDefinition> copied,
                                     boolean isAlias) {
        Tristate state = Tristate.TRUE();

        // We only have to check the keys if we have a context
        if (fragment.hasContext()) {
            ContextDefinition[] apCtxt = fragment.getContext();
            assert locations.length == apCtxt.length; // Failure must be because of a bad model

            for (int i = 0; i < locations.length && !state.isFalse(); i++) {
                ContextDefinition locFromStmt;
                int idx = locations[i].getParamIdx();
                switch (idx) {
                    case ParamIndex.ALL:
                        // Match any index
                        continue;
                    case ParamIndex.COPY:
                        // Write down index for later
                        if (copied != null)
                            copied.add(apCtxt[i]);
                        // Go to the next key
                        continue;
                    case ParamIndex.LAST_INDEX:
                        locFromStmt = isAlias ? strategy.getNextPosition(iie.getBase(), stmt)
                                : strategy.getLastPosition(iie.getBase(), stmt);
                        break;
                    case ParamIndex.FIRST_INDEX:
                        locFromStmt = strategy.getFirstPosition(iie.getBase(), stmt);
                        break;
                    default:
                        if (idx < 0)
                            throw new RuntimeException("Unknown location index supplied: " + idx);
                        if (locations[i].isValueBased())
                            locFromStmt = strategy.getIndexContext(iie.getArg(locations[i].getParamIdx()), stmt);
                        else
                            locFromStmt = strategy.getKeyContext(iie.getArg(locations[i].getParamIdx()), stmt);
                        break;
                }

                // Match statement location against context
                state = state.and(strategy.intersect(apCtxt[i], locFromStmt));
            }
        }

        return state;
    }

    protected AccessPath taintReturnValue(Value leftOp, ContextDefinition[] ctxt, String appendingField,
                                           Abstraction incoming, IContainerStrategy strategy, InfoflowManager manager) {
        AccessPathFragment[] oldFragments = incoming.getAccessPath().getFragments();
        AccessPathFragment[] fragments;
        if (appendingField == null) {
            fragments = new AccessPathFragment[oldFragments.length - 1];
            // Cut the first field, i.e. the container field
            System.arraycopy(oldFragments, 1, fragments, 0, fragments.length);
        } else {
            fragments = new AccessPathFragment[oldFragments.length];
            System.arraycopy(oldFragments, 1, fragments, 1, fragments.length - 1);
            fragments[0] = new AccessPathFragment(safeGetField(appendingField));
        }

        // Add the context if necessary
        if (ctxt != null && !strategy.shouldSmash(ctxt))
            fragments[0] = fragments[0].copyWithNewContext(ctxt);

        return manager.getAccessPathFactory().createAccessPath(leftOp, fragments,
                incoming.getAccessPath().getTaintSubFields());
    }

    /**
     * Create a new access path for a collection with a context
     *
     * @param base      base value, i.e. the collection local
     * @param ctxt      new context, possibly already smashed
     * @param oldAp     incoming access path
     * @param manager   infoflow manager
     * @return access path for the collection
     */
    protected AccessPath taintCollectionWithContext(Value base, ContextDefinition[] ctxt, AccessPath oldAp,
                                                    InfoflowManager manager) {
        AccessPathFragment[] oldFragments = oldAp.getFragments();
        int len = oldFragments == null ? 0 : oldFragments.length;
        AccessPathFragment[] fragments = new AccessPathFragment[len + 1];
        if (oldFragments != null)
            System.arraycopy(oldFragments, 0, fragments, 1, len);
        SootField f = safeGetField(field);
        fragments[0] = new AccessPathFragment(f, f.getType(), ctxt);
        return manager.getAccessPathFactory().createAccessPath(base, fragments, oldAp.getTaintSubFields());
    }
}
