package soot.jimple.infoflow.collections.strategies.appending;

import java.util.Arrays;
import java.util.Set;

import soot.SootField;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.CollectionTaintWrapper;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;

public class DefaultAppendingStrategy implements AppendingStrategy<Unit, Abstraction> {
    private final InfoflowManager manager;
    private final Set<SootField> contextFields;

    public DefaultAppendingStrategy(InfoflowManager manager, Set<SootField> contextFields, Set<String> methods) {
        this.manager = manager;
        this.contextFields = contextFields;
    }

    @Override
    public boolean hasContext(Abstraction abs) {
        return abs.getAccessPath().getFragmentCount() != 0
                && Arrays.stream(abs.getAccessPath().getFragments()).anyMatch(this::hasContext);
    }

    public boolean hasContext(AccessPathFragment f) {
        return contextFields.contains(f.getField());
    }

    @Override
    public boolean affectsContext(Unit unit) {
        return ((CollectionTaintWrapper) manager.getTaintWrapper()).isLocationDependent((Stmt) unit);
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
            if (!hasContext(fragment)) {
                newFragments[i] = fragment;
                continue;
            }

            for (int j = 0; j < origFragments.length; j++) {
                // We can't have two equal fragments because of symbolic access paths
                if (fragment.equals(origFragments[j])) {
                    newFragments[i] = targetFragments[j];
                    break;
                }
            }

            // If another summary (i.e. java.util.Set) uses the same field, we might
            // not have a matching field. So we do just keep this one.
            if (newFragments[i] == null)
                newFragments[i] = fragment;
        }

        AccessPath diffAp = manager.getAccessPathFactory().createAccessPath(d2Ap.getPlainValue(),
                                                                            d2Ap.getBaseType(),
                                                                            newFragments,
                                                                            d2Ap.getTaintSubFields(),
                                                                            false,
                                                                            true,
                                                                            d2Ap.getArrayTaintType());
        assert diffAp != null;
        return targetVal.deriveNewAbstraction(diffAp, null);
    }
}
