package soot.jimple.infoflow.collections.strategies.subsuming;

import java.util.Set;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;

public class CoarserSubsumingStrategy extends LargerContextSubsumingStrategy {
    public CoarserSubsumingStrategy(InfoflowManager manager, Set<String> methods) {
        super(manager, methods);
    }

    @Override
    public int compare(Abstraction a, Abstraction b) {
        // Prioritize empty contexts and then larger contexts
        int pA = getContextSize(a);
        int pB = getContextSize(b);
        // if both have no context, go by access path length
        if (pA == 0 && pB == 0)
            return Integer.compare(a.getAccessPath().getFragmentCount(), b.getAccessPath().getFragmentCount());
        if (pA == 0)
            return -1;
        if (pB == 0)
            return -1;
        if (pA <= pB)
            return -1;
        return 1;
    }
}
