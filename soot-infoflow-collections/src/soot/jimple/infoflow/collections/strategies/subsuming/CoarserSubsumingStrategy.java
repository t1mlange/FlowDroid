package soot.jimple.infoflow.collections.strategies.subsuming;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;

public class CoarserSubsumingStrategy extends LargerContextSubsumingStrategy {
    public CoarserSubsumingStrategy(InfoflowManager manager) {
        super(manager);
    }

    @Override
    public int compare(Abstraction a, Abstraction b) {
        if (a.getAccessPath().getFragmentCount() != a.getAccessPath().getFragmentCount())
            return Integer.compare(a.getAccessPath().getFragmentCount(), b.getAccessPath().getFragmentCount());

        return super.compare(a, b);
    }
}
