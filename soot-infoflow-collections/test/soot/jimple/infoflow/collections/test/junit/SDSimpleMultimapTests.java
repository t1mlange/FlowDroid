package soot.jimple.infoflow.collections.test.junit;

import java.io.File;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.collections.StubDroidBasedTaintWrapper;
import soot.jimple.infoflow.collections.parser.StubDroidSummaryProvider;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

public class SDSimpleMultimapTests extends SimpleMultimapTests {
    @Override
    protected void setConfiguration(InfoflowConfiguration config) {

    }

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
