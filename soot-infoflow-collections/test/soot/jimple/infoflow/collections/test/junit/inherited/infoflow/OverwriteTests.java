package soot.jimple.infoflow.collections.test.junit.inherited.infoflow;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.BeforeClass;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.CollectionTaintWrapper;
import soot.jimple.infoflow.collections.parser.CollectionXMLParser;
import soot.jimple.infoflow.collections.solver.fastSolver.CoarserReuseCollectionInfoflowSolver;
import soot.jimple.infoflow.collections.solver.fastSolver.executors.PriorityExecutorFactory;
import soot.jimple.infoflow.collections.test.junit.FlowDroidTests;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;

public class OverwriteTests extends soot.jimple.infoflow.test.junit.OverwriteTests {
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
				return FlowDroidTests.getSolver(executor, problem, solverConfig);
			}
		};
		CollectionXMLParser parser = new CollectionXMLParser();
		File dir = new File("collectionModels");
		try {
			for (File f : dir.listFiles())
				parser.parse(f.getPath());
		} catch (IOException e) {
			throw new RuntimeException("Parsing exception", e);
		}

		try {
			result.setTaintWrapper(new CollectionTaintWrapper(parser.getModels(), TaintWrapperFactory.createTaintWrapper()));
		} catch (URISyntaxException | IOException e) {
			throw new RuntimeException(e);
		}
		return result;
	}
}
