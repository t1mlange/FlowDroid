package soot.jimple.infoflow.collections.test.junit;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.Stmt;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
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

    private boolean containsStmtString(InfoflowResults res, String substr) {
        for (DataFlowResult r : res.getResultSet()) {
            for (Stmt s : r.getSource().getPath()) {
                if (s.toString().contains(substr))
                    return true;
            }
        }
        return false;
    }

    @Test//(timeout = 30000)
    public void testShiftOnAlias1() {
        IInfoflow infoflow = initInfoflow();
        infoflow.getConfig().getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
        Assert.assertFalse(containsStmtString(infoflow.getResults(), "get(int)>(0)"));
        Assert.assertTrue(containsStmtString(infoflow.getResults(), "get(int)>(1)"));
        Assert.assertFalse(containsStmtString(infoflow.getResults(), "get(int)>(2)"));

    }

    @Test//(timeout = 30000)
    public void testShiftOnAlias2() {
        IInfoflow infoflow = initInfoflow();
        infoflow.getConfig().setAliasingAlgorithm(InfoflowConfiguration.AliasingAlgorithm.None);
        infoflow.getConfig().getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
        infoflow.setTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
        Assert.assertFalse(containsStmtString(infoflow.getResults(), "get(int)>(0)"));
        Assert.assertTrue(containsStmtString(infoflow.getResults(), "get(int)>(1)"));
        Assert.assertFalse(containsStmtString(infoflow.getResults(), "get(int)>(2)"));
    }

    @Test//(timeout = 30000)
    public void testShiftOnAlias3() {
        IInfoflow infoflow = initInfoflow();
        infoflow.getConfig().getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
        infoflow.setTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
        Assert.assertFalse(containsStmtString(infoflow.getResults(), "get(int)>(0)"));
        Assert.assertTrue(containsStmtString(infoflow.getResults(), "get(int)>(1)"));
        Assert.assertFalse(containsStmtString(infoflow.getResults(), "get(int)>(2)"));
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
