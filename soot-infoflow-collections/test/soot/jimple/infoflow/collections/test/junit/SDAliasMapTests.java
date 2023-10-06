package soot.jimple.infoflow.collections.test.junit;

import soot.jimple.infoflow.collections.StubDroidBasedTaintWrapper;
import soot.jimple.infoflow.collections.parser.StubDroidSummaryProvider;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

import java.io.File;

public class SDAliasMapTests extends AliasMapTests {
    @Override
    protected ITaintPropagationWrapper getTaintWrapper() {
        try {
            StubDroidSummaryProvider sp = new StubDroidSummaryProvider(new File("stubdroidBased"));
            sp.loadAdditionalSummaries("summariesManual");
            return new StubDroidBasedTaintWrapper(sp, TestConstantStrategy::new);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}
