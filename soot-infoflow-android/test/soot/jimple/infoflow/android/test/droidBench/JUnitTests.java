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
package soot.jimple.infoflow.android.test.droidBench;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.xmlpull.v1.XmlPullParserException;


import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.ImplicitFlowMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.methodSummary.data.provider.LazySummaryProvider;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.jimple.infoflow.results.InfoflowPerformanceData;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;

public class JUnitTests {
	enum TestResultMode {
		DROIDBENCH, // the actual expected values of droidbench
		FLOWDROID_BACKWARDS, // the values from FlowDroid backwards analysis, use to test regressions/fixes
		FLOWDROID_FORWARDS
	}

	protected final TestResultMode mode = TestResultMode.FLOWDROID_BACKWARDS;

	/**
	 * Analyzes the given APK file for data flows
	 * 
	 * @param fileName The full path and file name of the APK file to analyze
	 * @return The data leaks found in the given APK file
	 * @throws IOException            Thrown if the given APK file or any other
	 *                                required file could not be found
	 * @throws XmlPullParserException Thrown if the Android manifest file could not
	 *                                be read.
	 */
	public InfoflowResults analyzeAPKFile(String fileName) throws IOException, XmlPullParserException {
		return analyzeAPKFile(fileName, false);
	}

	/**
	 * Analyzes the given APK file for data flows
	 * 
	 * @param fileName The full path and file name of the APK file to analyze
	 * @param iccModel The full path and file name of the ICC model to use
	 * @return The data leaks found in the given APK file
	 * @throws IOException            Thrown if the given APK file or any other
	 *                                required file could not be found
	 * @throws XmlPullParserException Thrown if the Android manifest file could not
	 *                                be read.
	 */
	public InfoflowResults analyzeAPKFile(String fileName, String iccModel) throws IOException, XmlPullParserException {
		return analyzeAPKFile(fileName, iccModel, null);
	}

	/**
	 * Analyzes the given APK file for data flows
	 * 
	 * @param fileName            The full path and file name of the APK file to
	 *                            analyze
	 * @param enableImplicitFlows True if implicit flows shall be tracked, otherwise
	 *                            false
	 * @return The data leaks found in the given APK file
	 * @throws IOException            Thrown if the given APK file or any other
	 *                                required file could not be found
	 * @throws XmlPullParserException Thrown if the Android manifest file could not
	 *                                be read.
	 */
	public InfoflowResults analyzeAPKFile(String fileName, final boolean enableImplicitFlows)
			throws IOException, XmlPullParserException {
		return analyzeAPKFile(fileName, null, new AnalysisConfigurationCallback() {

			@Override
			public void configureAnalyzer(InfoflowAndroidConfiguration config) {
				config.setImplicitFlowMode(
						enableImplicitFlows ? ImplicitFlowMode.AllImplicitFlows : ImplicitFlowMode.NoImplicitFlows);
			}

		});
	}

	/**
	 * Interface that allows test cases to configure the analyzer for DroidBench
	 * 
	 * @author Steven Arzt
	 *
	 */
	public interface AnalysisConfigurationCallback {

		/**
		 * Method that is called to give the test case the chance to change the analyzer
		 * configuration
		 * 
		 * @param config The configuration object used by the analyzer
		 */
		public void configureAnalyzer(InfoflowAndroidConfiguration config);

	}

	/**
	 * Analyzes the given APK file for data flows
	 * 
	 * @param fileName       The full path and file name of the APK file to analyze
	 * @param iccModel       The full path and file name of the ICC model to use
	 * @param configCallback A callback that is invoked to allow the test case to
	 *                       change the analyzer configuration when necessary. Pass
	 *                       null to ignore the callback.
	 * @return The data leaks found in the given APK file
	 * @throws IOException            Thrown if the given APK file or any other
	 *                                required file could not be found
	 * @throws XmlPullParserException Thrown if the Android manifest file could not
	 *                                be read.
	 */
	public InfoflowResults analyzeAPKFile(String fileName, String iccModel,
			AnalysisConfigurationCallback configCallback) throws IOException, XmlPullParserException {
		String androidJars = System.getenv("ANDROID_JARS");
		if (androidJars == null)
			androidJars = System.getProperty("ANDROID_JARS");
		if (androidJars == null)
			throw new RuntimeException("Android JAR dir not set");
		System.out.println("Loading Android.jar files from " + androidJars);

		String droidBenchDir = System.getenv("DROIDBENCH");
		if (droidBenchDir == null)
			droidBenchDir = System.getProperty("DROIDBENCH");
		if (droidBenchDir == null) {
			File droidBenchFile = new File("DroidBench/apk");
			if (!droidBenchFile.exists())
				droidBenchFile = new File("../DroidBench/apk");
			if (droidBenchFile.exists())
				droidBenchDir = droidBenchFile.getAbsolutePath();
		}
		if (droidBenchDir == null)
			throw new RuntimeException("DroidBench dir not set");
		System.out.println("Loading DroidBench from " + droidBenchDir);

		SetupApplication setupApplication = new SetupApplication(androidJars,
				droidBenchDir + File.separator + fileName);

		// Find the taint wrapper file
		File taintWrapperFile = new File("EasyTaintWrapperSource.txt");
		if (!taintWrapperFile.exists())
			taintWrapperFile = new File("../soot-infoflow/EasyTaintWrapperSource.txt");

		// Make sure to apply the settings before we calculate entry points
		if (configCallback != null)
			configCallback.configureAnalyzer(setupApplication.getConfig());

		setupApplication.getConfig().setEnableArraySizeTainting(true);

		setupApplication.setTaintWrapper(new EasyTaintWrapper(taintWrapperFile));
//		try {
//			setupApplication.setTaintWrapper(new SummaryTaintWrapper(new LazySummaryProvider("summariesManual")));
//		} catch (Exception e) {
//
//		}
		configStr += "easy";
		setupApplication.getConfig().setDataFlowDirection(InfoflowConfiguration.DataFlowDirection.Backwards);
		configStr += "bw";
//		configStr += "fw";

		if (iccModel != null && iccModel.length() > 0) {
			setupApplication.getConfig().getIccConfig().setIccModel(iccModel);
		}
		InfoflowResults results = setupApplication.runInfoflow("SourcesAndSinks.txt");
		if (results != null)
			performanceData = results.getPerformanceData();
		return results;
	}

	InfoflowPerformanceData performanceData;
	String configStr = "";

	class TestResult {
		String category;
		String name;
		long infoflowEdges = -1;
		long aliasEdges = -1;

		TestResult(String name, String category) {
			this.name = name;
			this.category = category;
		}

		void set(long infoflowEdges, long aliasEdges) {
			this.infoflowEdges = infoflowEdges;
			this.aliasEdges = aliasEdges;
		}

		@Override
		public String toString() {
			return category + "," + name + "," + infoflowEdges + "," + aliasEdges + "\n";
		}
	}

	private static boolean log = true;

	@Rule
	public TestWatcher edgeEvaluation = new TestWatcher() {
		@Override
		protected void failed(Throwable e, Description description) {
			if (!log)
				return;
			String category = description.getClassName().replace("soot.jimple.infoflow.android.test.droidBench.", "");
			String name = description.getMethodName().replace("runTest", "");
			TestResult tr = new TestResult(name, category);
			tr.set(-1, -1);
			dumpToFile(tr);
		}

		@Override
		protected void succeeded(Description description) {
			if (!log)
				return;
			String category = description.getClassName().replace("soot.jimple.infoflow.android.test.droidBench.", "");
			String name = description.getMethodName().replace("runTest", "");

			TestResult tr = new TestResult(name, category);

			if (performanceData == null)
				tr.set(-1, -1);
			else
				tr.set(performanceData.getInfoflowPropagationCount(), performanceData.getAliasPropagationCount());

			dumpToFile(tr);
		}

		private void dumpToFile(TestResult tr) {
			try (FileWriter fw = new FileWriter(configStr + ".txt", true)) {
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(tr.toString());
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};
}
