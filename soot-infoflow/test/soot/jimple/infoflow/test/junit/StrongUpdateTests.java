package soot.jimple.infoflow.test.junit;

import org.junit.Test;
import soot.jimple.infoflow.IInfoflow;

import java.util.ArrayList;
import java.util.List;

public class StrongUpdateTests extends JUnitTests {
    @Test
    public void strongUpdateTest1() {
        IInfoflow infoflow = initInfoflow();
        List<String> epoints = new ArrayList<String>();
        epoints.add("<soot.jimple.infoflow.test.StrongUpdateTestCode: void strongUpdateTest1()>");
        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        negativeCheckInfoflow(infoflow);
    }
    @Test
    public void strongUpdateTest2() {
        IInfoflow infoflow = initInfoflow();
        List<String> epoints = new ArrayList<String>();
        epoints.add("<soot.jimple.infoflow.test.StrongUpdateTestCode: void strongUpdateTest2()>");
        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        checkInfoflow(infoflow, 1);
    }
    @Test
    public void strongUpdateTest3() {
        IInfoflow infoflow = initInfoflow();
        List<String> epoints = new ArrayList<String>();
        epoints.add("<soot.jimple.infoflow.test.StrongUpdateTestCode: void strongUpdateTest3()>");
        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        negativeCheckInfoflow(infoflow);
    }
    @Test
    public void strongUpdateTest4() {
        IInfoflow infoflow = initInfoflow();
        List<String> epoints = new ArrayList<String>();
        epoints.add("<soot.jimple.infoflow.test.StrongUpdateTestCode: void strongUpdateTest4()>");
        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        checkInfoflow(infoflow, 1);
    }
}
