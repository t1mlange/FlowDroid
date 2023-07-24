package soot.jimple.infoflow.collections.strategies.appending;

import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Arrays;
import java.util.Set;

public class DefaultAppendingStrategy implements AppendingStrategy<Unit, Abstraction> {
    private final InfoflowManager manager;
    private final Set<String> methods;

    public DefaultAppendingStrategy(InfoflowManager manager, Set<String> methods) {
        this.manager = manager;
        this.methods = methods;
    }

    @Override
    public boolean hasContext(Abstraction abs) {
        return abs.getAccessPath().getFragmentCount() != 0
                && Arrays.stream(abs.getAccessPath().getFragments()).anyMatch(AccessPathFragment::hasContext);
    }

    @Override
    public boolean affectsContext(Unit unit) {
        Stmt stmt = (Stmt) unit;
        return stmt.containsInvokeExpr() && methods.contains(stmt.getInvokeExpr().getMethod().getSubSignature());
    }

    @Override
    public Abstraction applyDiffOf(Abstraction d1, Abstraction d2, Abstraction targetVal) {
        // We have to talk about the same abstraction
        assert d1.getAccessPath().getPlainValue() == targetVal.getAccessPath().getPlainValue()
                && d1.getAccessPath().getFragmentCount() == targetVal.getAccessPath().getFragmentCount();

        AccessPath d2Ap = d2.getAccessPath();
        if (d2Ap.getFragmentCount() == 0) {
            return targetVal.deriveNewAbstraction(d2Ap, null);
        }

        AccessPath targetAp = targetVal.getAccessPath();
        int maxIdx = targetAp.getFragmentCount() - 1;
        AccessPathFragment[] newFragments = new AccessPathFragment[d2Ap.getFragmentCount()];
        for (int i = d2Ap.getFragmentCount() - 1; i >= 0; i--) {
            AccessPathFragment f = d2Ap.getFragments()[i];
            ContextDefinition[] ctxt = f.getContext();
            // Only if the fragment at the end has a context, we have to look for replacements, because...
            // case 1) there was a context dependent statement on the flow, then this method will never be called
            // case 2) the deref of a context-dependent field is independent of the key (i.e. iterator)
            if (ctxt != null) {
                // We might have to replace the context
                for (int j = maxIdx; j >= 0; j--) {
                    AccessPathFragment targetF = targetAp.getFragments()[j];
                    if (targetF.getField().equals(f.getField())) {
                        ctxt = targetF.getContext();
                        maxIdx = j;
                        break;
                    }
                }
            }
            newFragments[i] = f.copyWithNewContext(ctxt);
        }
        AccessPath diffAp = manager.getAccessPathFactory().createAccessPath(targetAp.getPlainValue(), newFragments, targetAp.getTaintSubFields());
        assert diffAp != null;
        return targetVal.deriveNewAbstraction(diffAp, null);
    }
}
