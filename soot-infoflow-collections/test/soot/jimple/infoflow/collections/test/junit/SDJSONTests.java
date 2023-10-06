package soot.jimple.infoflow.collections.test.junit;

import java.io.File;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.collections.StubDroidBasedTaintWrapper;
import soot.jimple.infoflow.collections.parser.StubDroidSummaryProvider;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;
import soot.jimple.infoflow.collections.test.JSONTestCode;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

public class SDJSONTests extends JSONTests {

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
