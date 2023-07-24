package soot.jimple.infoflow.collections.test.junit;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.solver.fastSolver.CoarserReuseCollectionInfoflowSolver;
import soot.jimple.infoflow.collections.solver.fastSolver.executors.PriorityExecutorFactory;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;

@Ignore // The solver is not used anyways
public class SummaryReuseTests extends FlowDroidTests {
    @Override
    protected IInfoflow initInfoflow() {
        AbstractInfoflow result = new Infoflow("", false, new DefaultBiDiICFGFactory()) {
            @Override
            protected IInfoflowSolver createDataFlowSolver(InterruptableExecutor executor,
                                                           AbstractInfoflowProblem problem, InfoflowConfiguration.SolverConfiguration solverConfig) {
                return new CoarserReuseCollectionInfoflowSolver(problem, executor);
            }
        };
        result.setExecutorFactory(new PriorityExecutorFactory());
        result.setThrowExceptions(true);
        result.setTaintWrapper(getTaintWrapper());
        setConfiguration(result.getConfig());

        return result;
    }

    @Override
    protected void setConfiguration(InfoflowConfiguration config) {
        // Some of the test cases are benign races. To enforce the correct processing order of edges,
        // we limit the number of threads to 1 for these tests.
        config.setMaxThreadNum(1);
    }

    private static final String testCodeClass = "soot.jimple.infoflow.collections.test.SummaryReuseTestCode";

    @Test(timeout = 30000)
    public void testListCoarserSummaryReuse1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
    }

    @Test(timeout = 30000)
    public void testListCoarserSummaryReuse2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
    }

    @Test(timeout = 30000)
    public void testListCoarserSummaryReuse3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
    }

    @Test(timeout = 30000)
    public void testNoTaintGetsLost() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
    }

    @Test(timeout = 30000)
    public void testNarrowingOnIdentity() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
    }
}
