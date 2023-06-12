package soot.jimple.infoflow.collections.test.junit;

import org.junit.Assert;
import org.junit.Test;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.*;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.StringResourcesResolver;
import soot.jimple.infoflow.collections.solver.fastSolver.CollectionInfoflowSolver;
import soot.jimple.infoflow.collections.solver.fastSolver.executors.PriorityExecutorFactory;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.util.DebugFlowFunctionTaintPropagationHandler;

import java.util.Collections;
import java.util.Set;

public class SummaryReuseTests extends FlowDroidTests {
    @Override
    protected IInfoflow initInfoflow() {
        AbstractInfoflow result = new Infoflow("", false, new DefaultBiDiICFGFactory()) {
            @Override
            protected void performCodeInstrumentationBeforeDCE(InfoflowManager manager,
                                                               Set<SootMethod> excludedMethods) {
                super.performCodeInstrumentationBeforeDCE(manager, excludedMethods);
                StringResourcesResolver res = new StringResourcesResolver();
                res.initialize(manager.getConfig());
                res.run(manager, excludedMethods, manager.getSourceSinkManager(), manager.getTaintWrapper());
            }

            @Override
            protected IInfoflowSolver createDataFlowSolver(InterruptableExecutor executor,
                                                           AbstractInfoflowProblem problem, InfoflowConfiguration.SolverConfiguration solverConfig) {
                return new CollectionInfoflowSolver(problem, executor);
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

    }

    private static final String testCodeClass = "soot.jimple.infoflow.collections.test.SummaryReuseTestCode";

    @Test//(timeout = 30000)
    public void testListCoarserSummaryReuse() {
        IInfoflow infoflow = initInfoflow();
        infoflow.setTaintPropagationHandler(new TaintPropagationHandler() {
            @Override
            public void notifyFlowIn(Unit stmt, Abstraction taint, InfoflowManager manager, FlowFunctionType type) {

            }

            @Override
            public Set<Abstraction> notifyFlowOut(Unit stmt, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing, InfoflowManager manager, FlowFunctionType type) {
                // Delay the taint such that there definitely is a summary
                if (stmt.toString().contains("println")) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                return outgoing;
            }
        });
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }


    @Test//(timeout = 30000)
    public void testListCoarserSummaryReuse2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }


    @Test//(timeout = 30000)
    public void testSummary() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
    }
}
