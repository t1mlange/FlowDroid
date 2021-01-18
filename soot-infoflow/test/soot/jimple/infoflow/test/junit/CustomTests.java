package soot.jimple.infoflow.test.junit;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;

/**
 * Simple test cases to debug specific behavior
 *
 * @author Tim Lange
 *
 */
public class CustomTests extends JUnitTests {
    @Test
    public void testLList() {
        IInfoflow infoflow = initInfoflow();
        List<String> epoints = new ArrayList<String>();
        epoints.add("<soot.jimple.infoflow.test.CustomTestCode: void testLList()>");
        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        checkInfoflow(infoflow, 3);
    }
    @Test
    public void testLListStatic() {
        IInfoflow infoflow = initInfoflow();
        List<String> epoints = new ArrayList<String>();
        epoints.add("<soot.jimple.infoflow.test.CustomTestCode: void testLListStatic()>");
        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        checkInfoflow(infoflow, 3);
    }

    @Test
    public void clinitLeak() {
        IInfoflow infoflow = initInfoflow();
        List<String> epoints = new ArrayList<String>();
        epoints.add("<soot.jimple.infoflow.test.CustomTestCode: void clinitLeak()>");
        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        checkInfoflow(infoflow, 1);
    }

    @Test
    public void bookkeepingTest() {
        sources.add("<soot.jimple.infoflow.test.CustomTestCode: soot.jimple.infoflow.test.IntegerRef source()>");

        IInfoflow infoflow = initInfoflow();
        List<String> epoints = new ArrayList<String>();
        epoints.add("<soot.jimple.infoflow.test.CustomTestCode: void bookkeepingTest()>");
        infoflow.getConfig().setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        checkInfoflow(infoflow, 1);
    }
}
