package soot.jimple.infoflow.collections.test.junit;

import org.junit.Assert;
import org.junit.Test;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.util.DebugFlowFunctionTaintPropagationHandler;

import java.util.Collections;

public class ConstantKeyTableTests extends FlowDroidTests {
    @Override
    protected void setConfiguration(InfoflowConfiguration config) {

    }

    private static final String testCodeClass = "soot.jimple.infoflow.collections.test.ConstantKeyTableTestCode";

    @Test(timeout = 30000)
    public void testTablePutGet1() {
        IInfoflow infoflow = initInfoflow();
        infoflow.setTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTablePutGet2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTablePutGet3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTablePutRemove1() {
        IInfoflow infoflow = initInfoflow();
        infoflow.setTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTablePutRemove2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTablePutRemove3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTablePutAll1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTablePutAll2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableRow1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableRow2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableRow3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableColumn1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableColumn2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableColumn3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableCellSet1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableCellSet2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableColumnMap1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableColumnMap2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableRowMap1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testTableRowMap2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }
}
