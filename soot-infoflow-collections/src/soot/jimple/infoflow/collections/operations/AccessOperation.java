package soot.jimple.infoflow.collections.operations;

import soot.SootField;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.data.ParamIndex;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AccessOperation extends LocationDependentOperation {
    private final String returnField;

    public AccessOperation(Location[] keys, String field, String returnField) {
        super(keys, field);
        this.returnField = returnField;
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt,
                         InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        // No need to model an access if the return value is ignored
        if (!(stmt instanceof AssignStmt))
            return false;

        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getBase()))
            return false;

        AccessPathFragment fragment = incoming.getAccessPath().getFirstFragment();
        if (fragment == null || !fragment.getField().getSignature().equals(this.field))
            return false;

        Tristate state = Tristate.TRUE();
        List<ContextDefinition> copy = null;

        // We only have to check the keys if we have a context
        if (fragment.hasContext()) {
            ContextDefinition[] apCtxt = fragment.getContext();
            assert locations.length == apCtxt.length; // Failure must be because of a bad model

            for (int i = 0; i < locations.length && !state.isFalse(); i++) {
                ContextDefinition stmtKey;
                if (locations[i].getParamIdx() == ParamIndex.LAST_INDEX.toInt())
                    stmtKey = strategy.getLastPosition(iie.getBase(), stmt);
                else if (locations[i].getParamIdx() == ParamIndex.FIRST_INDEX.toInt())
                    stmtKey = strategy.getFirstPosition(iie.getBase(), stmt);
                else if (locations[i].getParamIdx() == ParamIndex.ALL.toInt())
                    continue;
                else if (locations[i].getParamIdx() == ParamIndex.COPY.toInt()) {
                    if (copy == null)
                        copy = new ArrayList<>();
                    copy.add(apCtxt[i]);
                    continue;
                } else if (locations[i].getParamIdx() >= 0)
                    if (locations[i].isValueBased())
                        stmtKey = strategy.getIndexContext(iie.getArg(locations[i].getParamIdx()), stmt);
                    else
                        stmtKey = strategy.getKeyContext(iie.getArg(locations[i].getParamIdx()), stmt);
                else
                    throw new RuntimeException("Wrong key supplied");
                state = state.and(strategy.intersect(apCtxt[i], stmtKey));
            }
        }

        if (!state.isFalse()) {
            Value leftOp = ((AssignStmt) stmt).getLeftOp();
            AccessPathFragment[] oldFragments = incoming.getAccessPath().getFragments();
            AccessPathFragment[] fragments;
            if (returnField == null) {
                fragments = new AccessPathFragment[oldFragments.length - 1];
                System.arraycopy(oldFragments, 1, fragments, 0, fragments.length);
            } else {
                fragments = new AccessPathFragment[oldFragments.length];
                System.arraycopy(oldFragments, 1, fragments, 1, fragments.length - 1);
                fragments[0] = new AccessPathFragment(safeGetField(returnField));
            }
            if (copy != null)
                fragments[0] = fragments[0].copyWithNewContext(copy.toArray(new ContextDefinition[0]));

            AccessPath ap = manager.getAccessPathFactory().createAccessPath(leftOp, fragments,
                    incoming.getAccessPath().getTaintSubFields());
            out.add(incoming.deriveNewAbstraction(ap, stmt));
        }

        // Access never removes an element
        return false;
    }
}
