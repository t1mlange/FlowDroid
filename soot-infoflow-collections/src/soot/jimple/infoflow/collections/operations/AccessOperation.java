package soot.jimple.infoflow.collections.operations;

import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Collection;

public class AccessOperation extends AbstractOperation {
    private final int[] keys;
    private final String field;
    private final String fieldType;


    public AccessOperation(int[] keys, String field, String fieldType) {
        this.keys = keys;
        this.field = field;
        this.fieldType = fieldType;
    }

    @Override
    public boolean apply(Stmt stmt,
                         Abstraction incoming,
                         Collection<Abstraction> out,
                         InfoflowManager manager,
                         IContainerStrategy strategy) {
        // No need to model an access if the return value is ignored
        if (!(stmt instanceof AssignStmt))
            return false;

        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getBase()))
            return false;

        AccessPathFragment fragment = incoming.getAccessPath().getFirstFragment();
        if (!fragment.getField().getSignature().equals(this.field))
            return false;

        Tristate state = Tristate.TRUE();

        // We only have to check the keys if we have a context
        if (fragment.hasContext()) {
            ContextDefinition[] apCtxt = fragment.getContext();
            assert keys.length == apCtxt.length; // Failure must be because of a bad model

            for (int i = 0; i < keys.length && !state.isFalse(); i++) {
                ContextDefinition stmtKey;
                if (keys[i] == Index.LAST_INDEX.toInt())
                    stmtKey = strategy.getLastPosition(iie.getBase(), stmt);
                else if (keys[i] >= 0)
                    stmtKey = strategy.getContextFromKey(iie.getArg(keys[i]), stmt);
                else
                    throw new RuntimeException("Wrong key supplied");
                state = state.and(strategy.intersect(apCtxt[i], stmtKey));
            }
        }

        if (!state.isFalse()) {
            Value leftOp = ((AssignStmt) stmt).getLeftOp();
            AccessPathFragment[] oldFragments = incoming.getAccessPath().getFragments();
            AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length - 1];
            System.arraycopy(oldFragments, 1, fragments, 0, fragments.length);
            AccessPath ap = manager.getAccessPathFactory().createAccessPath(leftOp, fragments, incoming.getAccessPath().getTaintSubFields());
            out.add(incoming.deriveNewAbstraction(ap, stmt));
        }

        // Access never removes an element
        return false;
    }
}
