package soot.jimple.infoflow.collections.strategies.subsuming;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Arrays;

public class LargerContextSubsumingStrategy implements SubsumingStrategy<Abstraction> {
    private final InfoflowManager manager;

    public LargerContextSubsumingStrategy(InfoflowManager manager) {
        this.manager = manager;
    }

    private int getContextSize(Abstraction abs) {
        if (abs.getAccessPath().getFragmentCount() == 0)
            return 0;

        int size = 0;
        for (AccessPathFragment f : abs.getAccessPath().getFragments())
            if (f.hasContext())
                for (ContextDefinition c : f.getContext())
                    size += c instanceof IntervalContext ? ((IntervalContext) c).size() : 1;

        return -size;
    }

    @Override
    public int compare(Abstraction a, Abstraction b) {
        return Integer.compare(getContextSize(a), getContextSize(b));
    }

    @Override
    public boolean subsumes(Abstraction a, Abstraction b) {
        return b.entails(a);
    }

    @Override
    public boolean hasContext(Abstraction abs) {
        return abs.getAccessPath().getFragmentCount() != 0 && Arrays.stream(abs.getAccessPath().getFragments()).anyMatch(f -> f.hasContext());
    }

    @Override
    public Abstraction removeContext(Abstraction abs) {
        AccessPathFragment[] oldFragments = abs.getAccessPath().getFragments();
        AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length];
        for (int i = 0; i < fragments.length; i++) {
            fragments[i] = oldFragments[i].copyWithNewContext(null);
        }
        AccessPath ap = manager.getAccessPathFactory().createAccessPath(abs.getAccessPath().getPlainValue(), fragments,
                abs.getAccessPath().getTaintSubFields());
        return abs.deriveNewAbstraction(ap, null);

    }
}
