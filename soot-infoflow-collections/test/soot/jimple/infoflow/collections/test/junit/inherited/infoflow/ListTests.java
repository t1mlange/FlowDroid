package soot.jimple.infoflow.collections.test.junit.inherited.infoflow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.CollectionTaintWrapper;
import soot.jimple.infoflow.collections.parser.CollectionXMLParser;
import soot.jimple.infoflow.collections.solver.fastSolver.CoarserReuseCollectionInfoflowSolver;
import soot.jimple.infoflow.collections.solver.fastSolver.executors.PriorityExecutorFactory;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;

public class ListTests extends soot.jimple.infoflow.test.junit.ListTests {
	@BeforeClass
	public static void setUp() throws IOException {
		soot.jimple.infoflow.test.junit.JUnitTests.setUp();
		File f = new File("../soot-infoflow");
		File testSrc = new File(f, "build" + File.separator + "testclasses");
		StringBuilder sb = new StringBuilder();
		if (appPath != null)
			sb.append(appPath);
		appendWithSeparator(sb, testSrc);
		appPath = sb.toString();
	}

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		AbstractInfoflow result = new Infoflow("", false, new DefaultBiDiICFGFactory()) {
			@Override
			protected IInfoflowSolver createDataFlowSolver(InterruptableExecutor executor,
														   AbstractInfoflowProblem problem, InfoflowConfiguration.SolverConfiguration solverConfig) {
				return new CoarserReuseCollectionInfoflowSolver(problem, executor);
			}
		};
		result.setExecutorFactory(new PriorityExecutorFactory());
		CollectionXMLParser parser = new CollectionXMLParser();
		File dir = new File("collectionModels");
		try {
			for (File f : dir.listFiles())
				parser.parse(f.getPath());
		} catch (IOException e) {
			throw new RuntimeException("Parsing exception", e);
		}
		result.setTaintWrapper(new CollectionTaintWrapper(parser.getModels(), null));
		return result;
	}

	@Test(timeout = 300000)
	@Override
	public void listIteratorTest() {

	}

	@Test(timeout = 300000)
	@Override
	public void listToStringTest() {
		// No toString summary
	}

	@Test(timeout = 300000)
	@Override
	public void concreteArrayListPos0Test() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ListTestCode: void concreteWriteReadPos0Test()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		// We are more precise
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	@Override
	public void staticLinkedListIteratorTest() {
		// TODO: what about Collection.add()?
//		IInfoflow infoflow = initInfoflow();
//		infoflow.setTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
//		List<String> epoints = new ArrayList<String>();
//		epoints.add("<soot.jimple.infoflow.test.ListTestCode: void staticLinkedList()>");
//		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
//		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 250000)
	@Override
	public void linkedListIteratorTest() {
		// TODO: what about Collection.add()?
	}

	@Test(timeout = 300000)
	public void concreteLinkedListIteratorTest() {
		// TODO: what about Collection.add()?
	}
}
