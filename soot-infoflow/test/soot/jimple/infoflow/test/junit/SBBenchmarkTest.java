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

import org.junit.Assert;
import org.junit.Test;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.options.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * covers taint propagation for Strings, String functions such as concat,
 * toUpperCase() or substring, but also StringBuilder and concatenation via '+'
 * operato
 * 
 * @author Christian
 *
 */
public class SBBenchmarkTest extends JUnitTests {

	@Test(timeout = 600000)
	public void appendTest() {
		IInfoflow infoflow = initInfoflow();
//		infoflow.getConfig().setAliasingAlgorithm(InfoflowConfiguration.AliasingAlgorithm.None);
		try {
			infoflow.setTaintWrapper(new EasyTaintWrapper(new File("EasyTaintWrapperSource.txt")));
		} catch (Exception e) {}
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.SBBenchmarkCode: void appendTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}
}
