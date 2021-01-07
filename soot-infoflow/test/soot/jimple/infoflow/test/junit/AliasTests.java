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

import org.junit.Test;
import soot.jimple.infoflow.IInfoflow;

import java.util.ArrayList;
import java.util.List;


public class AliasTests extends JUnitTests {
	@Test
	public void testAliasBefore1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.AliasTestCode: void testAliasBefore1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void testAliasBefore2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.AliasTestCode: void testAliasBefore2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void testAliasAfterPositive() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.AliasTestCode: void testAliasAfterPositive()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void testAliasAfterNegative() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.AliasTestCode: void testAliasAfterNegative()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test
	public void testAliasPrimitiveFieldPositive() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.AliasTestCode: void testAliasPrimitiveFieldPositive()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void testAliasPrimitiveFieldNegative() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.AliasTestCode: void testAliasPrimitiveFieldNegative()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}
}
