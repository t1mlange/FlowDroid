package soot.jimple.infoflow.collections.strategies.subsuming;

import java.util.Arrays;
import java.util.Set;

import soot.Unit;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

@Deprecated
public class LargerContextSubsumingStrategy implements SubsumingStrategy<Unit, Abstraction> {
    private final InfoflowManager manager;

    public LargerContextSubsumingStrategy(InfoflowManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean hasContext(Abstraction abs) {
        return abs.getAccessPath().getFragmentCount() != 0
                && Arrays.stream(abs.getAccessPath().getFragments()).anyMatch(AccessPathFragment::hasContext);
    }

    protected int getContextSize(Abstraction abs) {
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
        // Prioritize empty contexts and then larger contexts
        int pA = getContextSize(a);
        if (pA == 0)
            return -1;
        int pB = getContextSize(b);
        if (pB == 0)
            return -1;
        if (pA <= pB)
            return -1;
        return 1;
    }

    @Override
    public boolean subsumes(Abstraction a, Abstraction b) {
        return b.entails(a);
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

    @Override
    public Abstraction chooseContext(Set<Abstraction> availableContexts, Abstraction currentContext) {
        Abstraction curr = null;
        for (Abstraction sumD1 : availableContexts) {
            // We don't have a summary, so we need a coarser summary...
            if (sumD1.entails(currentContext)) {
                // ...but from all coarser summaries, we want the closest to the current context
                if (curr == null || curr.entails(sumD1))
                    curr = sumD1;
            }
        }

        return curr;
    }
}
