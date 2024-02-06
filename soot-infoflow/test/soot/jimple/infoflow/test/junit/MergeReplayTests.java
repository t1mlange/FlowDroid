/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.test.junit;

import java.util.*;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.AliasingAlgorithm;
import soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.SequentialEntryPointCreator;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;
import soot.jimple.infoflow.taintWrappers.AbstractTaintWrapper;
import soot.jimple.infoflow.taintWrappers.IReversibleTaintWrapper;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.util.DebugFlowFunctionTaintPropagationHandler;

/**
 * tests aliasing of heap references
 */
public abstract class MergeReplayTests extends JUnitTests {
	@Test(timeout = 300000)
	public void replaceAUWithGAU1() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().getSolverConfiguration().setFlowSensitivityMode(InfoflowConfiguration.FlowSensitivityMode.MergeReplay);
		List<String> epoints = new ArrayList<String>();
		infoflow.setAliasPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
		epoints.add("<soot.jimple.infoflow.test.MergeReplayTestCode: void replaceAUWithGAU1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().numConnections());
	}

	@Test(timeout = 300000)
	public void duplicatePropagation2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().getSolverConfiguration().setFlowSensitivityMode(InfoflowConfiguration.FlowSensitivityMode.MergeReplay);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MergeReplayTestCode: void duplicatePropagation2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(2, infoflow.getResults().numConnections());
	}

	@Test//(timeout = 300000)
	public void duplicatePropagation1() {
		while (true) {
			IInfoflow infoflow = initInfoflow();
//		infoflow.getConfig().getSolverConfiguration().setFlowSensitivityMode(InfoflowConfiguration.FlowSensitivityMode.MergeReplay);
			List<String> epoints = new ArrayList<String>();
//			infoflow.setAliasPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
			epoints.add("<soot.jimple.infoflow.test.MergeReplayTestCode: void duplicatePropagation1()>");
			infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
			checkInfoflow(infoflow, 1);
			Assert.assertEquals(1, infoflow.getResults().numConnections());
		}
	}

	@Test(timeout = 300000)
	public void duplicatePropagationAndFP1() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().getSolverConfiguration().setFlowSensitivityMode(InfoflowConfiguration.FlowSensitivityMode.MergeReplay);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MergeReplayTestCode: void duplicatePropagationAndFP1()>");
		infoflow.setAliasPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);

		checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().numConnections());
	}

	@Test(timeout = 300000)
	public void neighborProblem() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.MergeReplayTestCode: void neighborProblem()>");
		infoflow.setAliasPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);

		checkInfoflow(infoflow, 1);
		Assert.assertEquals(1, infoflow.getResults().numConnections());
	}
}
