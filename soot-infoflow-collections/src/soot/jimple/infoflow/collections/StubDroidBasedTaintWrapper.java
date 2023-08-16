package soot.jimple.infoflow.collections;

import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;

public class StubDroidBasedTaintWrapper extends SummaryTaintWrapper {
    /**
     * Creates a new instance of the {@link SummaryTaintWrapper} class
     *
     * @param flows The flows loaded from disk
     */
    public StubDroidBasedTaintWrapper(IMethodSummaryProvider flows) {
        super(flows);
    }
}
