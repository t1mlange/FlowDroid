package soot.jimple.infoflow.collections.test.junit.inherited.infoflow;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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
import soot.jimple.infoflow.collections.test.junit.FlowDroidTests;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;

public class MapTests extends soot.jimple.infoflow.test.junit.MapTests {
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

	@Test(timeout = 300000)
	public void mapPos0Test() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MapTestCode: void writeReadPos0Test()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		// We are more precise \o/
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void concreteMapPos1Test() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MapTestCode: void concreteWriteReadPos1Test()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		// We are more precise \o/
		negativeCheckInfoflow(infoflow);
	}
}
