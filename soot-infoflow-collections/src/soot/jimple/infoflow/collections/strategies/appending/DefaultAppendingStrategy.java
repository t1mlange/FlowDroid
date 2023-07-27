package soot.jimple.infoflow.collections.strategies.appending;

import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;

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
        AccessPath targetAp = targetVal.getAccessPath();
        AccessPath d1Ap = d1.getAccessPath();
        // We have to talk about the same abstraction
        assert d1Ap.getPlainValue() == targetAp.getPlainValue() && d1Ap.getFragmentCount() == targetAp.getFragmentCount();

        AccessPath d2Ap = d2.getAccessPath();
        if (d2Ap.getFragmentCount() == 0) {
            return targetVal.deriveNewAbstraction(d2Ap, null);
        }

        AccessPathFragment[] targetFragments = targetAp.getFragments();
        AccessPathFragment[] origFragments = d1Ap.getFragments();
        AccessPathFragment[] endFragments = d2Ap.getFragments();

        AccessPathFragment[] newFragments = new AccessPathFragment[d2Ap.getFragmentCount()];
        // Replace the contexts left from the calling context with the contexts from the appended abstraction
        for (int i = 0; i < endFragments.length; i++) {
            AccessPathFragment fragment = endFragments[i];
            if (!fragment.hasContext()) {
                newFragments[i] = fragment;
                continue;
            }

            for (int j = 0; j < origFragments.length; j++) {
                if (fragment == origFragments[j]) {
                    newFragments[i] = targetFragments[j];
                    break;
                }
            }
        }

        AccessPath diffAp = manager.getAccessPathFactory().createAccessPath(d2Ap.getPlainValue(),
                                                                            newFragments,
                                                                            d2Ap.getTaintSubFields());
        assert diffAp != null;
        return targetVal.deriveNewAbstraction(diffAp, null);
    }
}
