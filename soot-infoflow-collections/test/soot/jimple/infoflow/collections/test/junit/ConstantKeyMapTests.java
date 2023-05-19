package soot.jimple.infoflow.collections.test.junit;

import org.junit.Assert;
import org.junit.Test;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.util.DebugFlowFunctionTaintPropagationHandler;

import java.util.Collections;

public class ConstantKeyMapTests extends FlowDroidTests {
    @Override
    protected void setConfiguration(InfoflowConfiguration config) {

    }

    private static final String testCodeClass = "soot.jimple.infoflow.collections.test.ConstantKeyMapTestCode";

    @Test(timeout = 30000)
    public void testMapPutGet1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutGet2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutGetOrDefault1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutGetOrDefault2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutGetOrDefault3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutRemoveGet1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapClear1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapKeySet1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapKeySet2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapValueSet1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutAll1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutAll2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutIfAbsent1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutIfAbsent2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutIfAbsent3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutIfAbsent4() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapPutIfAbsent5() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        // An execution would yield no leak, but we have to over-approximate putIfAbsent
        // because a precise flow function would not be distributive here.
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test//(timeout = 30000)
    public void testMapCompute1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapCompute2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapCompute3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapCompute4() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapComputeIfAbsent1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapComputeIfAbsent2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test//(timeout = 30000)
    public void testMapMerge1() {
        IInfoflow infoflow = initInfoflow();
        infoflow.setTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapMerge2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapMerge3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapMerge4() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapMerge5() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapMerge6() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapMerge7() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapMerge8() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapReplaceAll1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(1, infoflow.getResults().size());
    }

    @Test(timeout = 30000)
    public void testMapReplaceAll2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(0, infoflow.getResults().size());
    }
}
