package soot.jimple.infoflow.collections.test.junit;

import java.io.File;

import soot.jimple.infoflow.collections.StubDroidBasedTaintWrapper;
import soot.jimple.infoflow.collections.parser.StubDroidSummaryProvider;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

public class SDAliasListTests extends AliasListTests {
    @Override
    protected ITaintPropagationWrapper getTaintWrapper() {
        try {
            StubDroidSummaryProvider sp = new StubDroidSummaryProvider(new File("stubdroidBased"));
            sp.loadAdditionalSummaries("summariesManual");
            return new StubDroidBasedTaintWrapper(sp);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}
