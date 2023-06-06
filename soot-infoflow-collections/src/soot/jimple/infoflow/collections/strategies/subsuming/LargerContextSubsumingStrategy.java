package soot.jimple.infoflow.collections.strategies.subsuming;

import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

public class LargerContextSubsumingStrategy implements SubsumingStrategy<Abstraction> {
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
}
