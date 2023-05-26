package soot.jimple.infoflow.collections.test.junit;

import org.checkerframework.checker.units.qual.C;
import org.junit.Assert;
import org.junit.Before;
import soot.SootMethod;
import soot.jimple.infoflow.*;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.collections.CollectionTaintWrapper;
import soot.jimple.infoflow.collections.StringResourcesResolver;
import soot.jimple.infoflow.collections.parser.CollectionXMLParser;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Abstract test class used for all river tests
 *
 * @author Tim Lange
 */
public abstract class FlowDroidTests {
    protected static String appPath, libPath;

    protected static List<String> sources = Collections.singletonList("<soot.jimple.infoflow.collections.test.Helper: java.lang.String source()>");
    protected static List<String> sinks = Collections.singletonList("<soot.jimple.infoflow.collections.test.Helper: void sink(java.lang.String)>");

    static void appendWithSeparator(StringBuilder sb, File f) throws IOException {
        if (f.exists()) {
            if (sb.length() > 0)
                sb.append(System.getProperty("path.separator"));
            sb.append(f.getCanonicalPath());
        }
    }

    protected static String getCurrentMethod() {
        return Thread.currentThread().getStackTrace()[2].getMethodName();
    }

    @Before
    public void resetSootAndStream() throws IOException {
        soot.G.reset();
        System.gc();
        commonSetup();
    }

    public static void commonSetup() throws IOException {
        File testSrc = new File("build" + File.separator + "testclasses");
        if (!testSrc.exists()) {
            Assert.fail("Test aborted - none of the test sources are available");
        }
        appPath = testSrc.toString();

        StringBuilder libPathBuilder = new StringBuilder();
        appendWithSeparator(libPathBuilder,
                new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar"));
        appendWithSeparator(libPathBuilder, new File("/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar"));
        libPath = libPathBuilder.toString();
    }

    protected ITaintPropagationWrapper getTaintWrapper() {
        try {
            SummaryTaintWrapper tw = TaintWrapperFactory.createTaintWrapper(Collections.singleton("../soot-infoflow-summaries/summariesManual"));
            CollectionXMLParser parser = new CollectionXMLParser();
            File dir = new File("collectionModels");
            for (File f : dir.listFiles())
                parser.parse(f.getPath());
            return new CollectionTaintWrapper(parser.getModels(), tw);
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException("Could not initialized Taintwrapper:");
        }
    }

    protected abstract void setConfiguration(InfoflowConfiguration config);

    protected IInfoflow initInfoflow() {
        AbstractInfoflow result = new Infoflow("", false, new DefaultBiDiICFGFactory()) {
            @Override
            protected void performCodeInstrumentationBeforeDCE(InfoflowManager manager, Set<SootMethod> excludedMethods) {
                super.performCodeInstrumentationBeforeDCE(manager, excludedMethods);
                StringResourcesResolver res = new StringResourcesResolver();
                res.initialize(manager.getConfig());
                res.run(manager, excludedMethods, manager.getSourceSinkManager(), manager.getTaintWrapper());
            }
        };
        result.setThrowExceptions(true);
        result.setTaintWrapper(getTaintWrapper());
        setConfiguration(result.getConfig());
        return result;
    }

    protected SetupApplication initApplication(String fileName) {
        String androidJars = System.getenv("ANDROID_JARS");
        if (androidJars == null)
            androidJars = System.getProperty("ANDROID_JARS");
        if (androidJars == null)
            throw new RuntimeException("Android JAR dir not set");
        System.out.println("Loading Android.jar files from " + androidJars);

        SetupApplication setupApplication = new SetupApplication(androidJars, fileName);
        setupApplication.addOptimizationPass(new SetupApplication.OptimizationPass() {
            @Override
            public void performCodeInstrumentationBeforeDCE(InfoflowManager manager, Set<SootMethod> excludedMethods) {
                StringResourcesResolver res = new StringResourcesResolver();
                res.initialize(manager.getConfig());
                res.run(manager, excludedMethods, manager.getSourceSinkManager(), manager.getTaintWrapper());
            }

            @Override
            public void performCodeInstrumentationAfterDCE(InfoflowManager manager, Set<SootMethod> excludedMethods) {

            }
        });
        setupApplication.getConfig().setMergeDexFiles(true);
        setupApplication.setTaintWrapper(getTaintWrapper());
        setConfiguration(setupApplication.getConfig());

        return setupApplication;
    }
}
