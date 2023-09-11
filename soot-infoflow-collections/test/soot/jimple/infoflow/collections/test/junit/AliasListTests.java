package soot.jimple.infoflow.collections.test.junit;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.util.DebugFlowFunctionTaintPropagationHandler;

public class AliasListTests extends FlowDroidTests {
    @Override
    protected void setConfiguration(InfoflowConfiguration config) {

    }

    private static final String testCodeClass = "soot.jimple.infoflow.collections.test.AliasListTestCode";

    @Test(timeout = 30000)
    public void testListAdd1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test//(timeout = 30000)
    public void testShiftOnAlias1() {
        IInfoflow infoflow = initInfoflow();
        infoflow.getConfig().getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test//(timeout = 30000)
    public void testShiftOnAlias2() {
        IInfoflow infoflow = initInfoflow();
        infoflow.getConfig().getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
        infoflow.setTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Test//(timeout = 30000)
    public void testShiftOnAlias3() {
        IInfoflow infoflow = initInfoflow();
        infoflow.getConfig().getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
        infoflow.setTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }

    @Ignore("The simplistic list size analysis overapproximates lists with aliases")
    @Test(timeout = 30000)
    public void testListShiftAlias1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }
}
