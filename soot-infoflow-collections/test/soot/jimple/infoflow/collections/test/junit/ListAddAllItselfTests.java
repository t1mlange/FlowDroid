package soot.jimple.infoflow.collections.test.junit;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.CollectionTaintWrapper;
import soot.jimple.infoflow.collections.StubDroidBasedTaintWrapper;
import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.parser.CollectionXMLParser;
import soot.jimple.infoflow.collections.parser.StubDroidSummaryProvider;
import soot.jimple.infoflow.collections.strategies.containers.TestConstantStrategy;
import soot.jimple.infoflow.data.ContextDefinition;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.util.DebugFlowFunctionTaintPropagationHandler;

import javax.xml.stream.XMLStreamException;

public class ListAddAllItselfTests extends FlowDroidTests {


    protected ITaintPropagationWrapper getTaintWrapper() {
        try {
            StubDroidSummaryProvider sp = new StubDroidSummaryProvider(new File("stubdroidBased"));
            sp.loadAdditionalSummaries("summariesManual");
            StubDroidBasedTaintWrapper sbtw = new StubDroidBasedTaintWrapper(sp) {
                @Override
                public void initialize(InfoflowManager manager) {
                    super.initialize(manager);
                    this.containerStrategy = new TestConstantStrategy(manager) {
                        @Override
                        public ContextDefinition getNextPosition(Value value, Stmt stmt) {
                            if (stmt.toString().contains("addAll("))
                                return new IntervalContext(3);

                            if (stmt.toString().contains("add("))
                                return new IntervalContext(0);

                            return UnknownContext.v();
                        }
                    };
                }
            };

            return sbtw;
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @Override
    protected void setConfiguration(InfoflowConfiguration config) {

    }

    private static final String testCodeClass = "soot.jimple.infoflow.collections.test.ListAddAllItselfTestCode";

    @Test(timeout = 30000)
    public void testListAllAllItselfFiniteLoop1() {
        IInfoflow infoflow = initInfoflow();
        infoflow.setTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }
}
