package soot.jimple.infoflow.collections.test.junit;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.*;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.solver.fastSolver.AbstractingCollectionInfoflowSolver;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;
import soot.jimple.infoflow.handlers.SequentialTaintPropagationHandler;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.util.DebugFlowFunctionTaintPropagationHandler;

public class AbstractingTests extends FlowDroidTests {
    /**
     * TaintPropagationHandler that sleeps on System.out.println.
     * Use to enforce that an end summary is used
     */
    private static class DelayOnPrintln implements TaintPropagationHandler {
        @Override
        public void notifyFlowIn(Unit unit, Abstraction taint, InfoflowManager manager, FlowFunctionType type) {
            Stmt stmt = (Stmt) unit;
            if (!stmt.containsInvokeExpr())
                return;

            if (!stmt.getInvokeExpr().getMethod().getName().equals("println"))
                return;

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Assume.assumeTrue("Sleeping failed, test might not work!", true);
            }
        }

        @Override
        public Set<Abstraction> notifyFlowOut(Unit stmt, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing, InfoflowManager manager, FlowFunctionType type) {
            return outgoing;
        }
    }

    /**
     * Custom taint propagation handler that ensures a method was never visited with
     * two different context definitions
     */
    private static class EnsureOnlyOneContext implements TaintPropagationHandler {
        ContextDefinition[] seenContext = null;
        @Override
        public void notifyFlowIn(Unit stmt, Abstraction taint, InfoflowManager manager, TaintPropagationHandler.FlowFunctionType
        type) {
            // We do rewrite the abstractions after call flow and before return flow functions, thus,
            // both are allowed to see more than one context
            if (type == TaintPropagationHandler.FlowFunctionType.CallFlowFunction || type == TaintPropagationHandler.FlowFunctionType.ReturnFlowFunction)
                return;

            SootMethod sm = manager.getICFG().getMethodOf(stmt);
            if (!sm.getName().equals("unusedContext1"))
                return;

            AccessPathFragment f = taint.getAccessPath().getFirstFragment();
            if (f == null || !f.hasContext())
                return;

            synchronized (this) {
                if (seenContext == null)
                    seenContext = f.getContext();
                Assert.assertArrayEquals(seenContext, f.getContext());
            }
        }

        @Override
        public Set<Abstraction> notifyFlowOut(Unit stmt, Abstraction d1, Abstraction incoming,
                Set<Abstraction> outgoing, InfoflowManager manager,
                TaintPropagationHandler.FlowFunctionType type) {
            return outgoing;
        }
    }

    @Override
    protected void setConfiguration(InfoflowConfiguration config) {

    }

    @Override
    protected IInfoflow initInfoflow() {
        AbstractInfoflow result = new Infoflow("", false, new DefaultBiDiICFGFactory()) {
            @Override
            protected IInfoflowSolver createDataFlowSolver(InterruptableExecutor executor,
                                                           AbstractInfoflowProblem problem, InfoflowConfiguration.SolverConfiguration solverConfig) {
                return new AbstractingCollectionInfoflowSolver(problem, executor);
            }
        };
        result.setThrowExceptions(true);
        result.setTaintWrapper(getTaintWrapper());
        setConfiguration(result.getConfig());

        SequentialTaintPropagationHandler tpg = new SequentialTaintPropagationHandler();
        tpg.addHandler(new EnsureOnlyOneContext());
        tpg.addHandler(new DelayOnPrintln());
        result.setTaintPropagationHandler(tpg);

        return result;
    }

    private boolean hasDuplicateSinkInFlow(Set<DataFlowResult> set) {
        if (set == null || set.isEmpty())
            return false;

        Set<Stmt> sinks = new HashSet<>();
        for (DataFlowResult res : set) {
            if (!sinks.add(res.getSink().getStmt()))
                return true;
        }

        return false;
    }

    private boolean hasDuplicateSourceInFlow(Set<DataFlowResult> set) {
        if (set == null || set.isEmpty())
            return false;

        Set<Stmt> sources = new HashSet<>();
        for (DataFlowResult res : set) {
            if (!sources.add(res.getSource().getStmt()))
                return true;
        }

        return false;
    }

    private static final String testCodeClass = "soot.jimple.infoflow.collections.test.AbstractingTestCode";

    @Test(timeout = 30000)
    public void testReuse1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
        Assert.assertFalse(hasDuplicateSourceInFlow(set));
        Assert.assertFalse(hasDuplicateSinkInFlow(set));
    }

    @Test(timeout = 30000)
    public void testReuse2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
        Assert.assertFalse(hasDuplicateSourceInFlow(set));
        Assert.assertFalse(hasDuplicateSinkInFlow(set));
    }

    @Test(timeout = 30000)
    public void testReuse3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
        Assert.assertFalse(hasDuplicateSinkInFlow(set));
    }

    @Test(timeout = 30000)
    public void testReuse4() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
        Assert.assertFalse(hasDuplicateSinkInFlow(set));
    }

    @Test(timeout = 30000)
    public void testGet1() {
        IInfoflow infoflow = initInfoflow();
        infoflow.getConfig().setWriteOutputFiles(true);
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
        Assert.assertFalse(hasDuplicateSourceInFlow(set));
        Assert.assertFalse(hasDuplicateSinkInFlow(set));
    }

    @Test(timeout = 30000)
    public void testGet2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
        Assert.assertFalse(hasDuplicateSinkInFlow(set));
    }

    @Test(timeout = 30000)
    public void testRemove1() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
        Assert.assertFalse(hasDuplicateSourceInFlow(set));
        Assert.assertFalse(hasDuplicateSinkInFlow(set));
    }

    @Test(timeout = 30000)
    public void testRemove2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
        Assert.assertFalse(hasDuplicateSinkInFlow(set));
    }

    @Test//(timeout = 30000)
    public void testReinjectInCallee1() {
        while (true) {
            IInfoflow infoflow = initInfoflow();
        infoflow.setTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
            String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
            infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
            var set = infoflow.getResults().getResultSet();
            Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
            Assert.assertFalse(hasDuplicateSinkInFlow(set));
        }
    }

    @Test(timeout = 30000)
    public void testReinjectInCallee2() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
        Assert.assertFalse(hasDuplicateSinkInFlow(set));
    }

    @Test(timeout = 30000)
    public void testReinjectInCallee3() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
        Assert.assertFalse(hasDuplicateSinkInFlow(set));
    }

    @Test(timeout = 30000)
    public void testReinjectInCallee4() {
        IInfoflow infoflow = initInfoflow();
        String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
        infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
        var set = infoflow.getResults().getResultSet();
        Assert.assertEquals(getExpectedResultsForMethod(epoint), set == null ? 0 : set.size());
        Assert.assertFalse(hasDuplicateSinkInFlow(set));
    }
}
