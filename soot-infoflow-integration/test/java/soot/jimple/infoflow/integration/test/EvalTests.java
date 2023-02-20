package soot.jimple.infoflow.integration.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.SootMethod;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.BaseSourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class EvalTests {
    static class SimpleSourceSinkManager extends BaseSourceSinkManager {
        public SimpleSourceSinkManager(Collection<? extends ISourceSinkDefinition> sources,
                                       Collection<? extends ISourceSinkDefinition> sinks,
                                       InfoflowConfiguration config) {
            super(sources, sinks, config);
        }

        @Override
        protected boolean isEntryPointMethod(SootMethod method) {
            return false;
        }
    }

    protected static String appPath, libPath;

    protected static List<String> sources;
    protected static final String localSource = "<soot.jimple.infoflow.android.test.river.RiverTestCode: java.lang.String source()>";
    protected static final String localIntSource = "<soot.jimple.infoflow.android.test.river.RiverTestCode: int intSource()>";

    protected static List<String> primarySinks;
    protected static final String osWrite = "<java.io.OutputStream: void write(byte[])>";
    protected static final String osWriteInt = "<java.io.OutputStream: void write(int)>";
    protected static final String writerWrite = "<java.io.Writer: void write(java.lang.String)>";
    protected static final String bufosWrite = "<java.io.FilterOutputStream: void write(byte[])>";

    protected static final String sendToUrl = "<soot.jimple.infoflow.android.test.river.RiverTestCode: void sendToUrl(java.net.URL,java.lang.String)>";
    protected static final String uncondSink = "<soot.jimple.infoflow.android.test.river.RiverTestCode: void unconditionalSink(java.lang.String)>";


    protected static final String urlInit = "<java.net.URL: void <init>(java.lang.String)>";

    private static void appendWithSeparator(StringBuilder sb, File f) throws IOException {
        if (f.exists()) {
            if (sb.length() > 0)
                sb.append(System.getProperty("path.separator"));
            sb.append(f.getCanonicalPath());
        }
    }

    @Before
    public void resetSootAndStream() throws IOException {
        soot.G.reset();
        System.gc();

    }

    @BeforeClass
    public static void setUp() throws IOException {
        File testSrc = new File("./build" + File.separator + "testclasses");
        if (!testSrc.exists()) {
            Assert.fail("Test aborted - none of the test sources are available");
        }
        appPath = testSrc.toString();

        StringBuilder libPathBuilder = new StringBuilder();
        appendWithSeparator(libPathBuilder,
                new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar"));
        appendWithSeparator(libPathBuilder, new File("/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar"));
        libPath = libPathBuilder.toString();

        sources = new ArrayList<String>();
        sources.add(localSource);
        sources.add(localIntSource);

        primarySinks = new ArrayList<String>();
        primarySinks.add(osWrite);
        primarySinks.add(osWriteInt);
        primarySinks.add(writerWrite);
        primarySinks.add(sendToUrl);
        primarySinks.add(uncondSink);
        primarySinks.add(bufosWrite);
    }

    protected IInfoflow initInfoflow() {
        AbstractInfoflow result = new Infoflow("", false, new DefaultBiDiICFGFactory());
        result.setThrowExceptions(true);
        try {
            SummaryTaintWrapper summaryWrapper = TaintWrapperFactory.createTaintWrapper(Collections.singleton("../soot-infoflow-summaries/summariesManual"));
            result.setTaintWrapper(summaryWrapper);
        } catch (IOException | XMLStreamException e) {
            System.err.println("Could not initialized Taintwrapper:");
            e.printStackTrace();
        }
        result.getConfig().setAdditionalFlowsEnabled(true);
        result.getConfig().getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
        return result;
    }

    protected void checkInfoflow(IInfoflow infoflow, int resultCount) {
        if (infoflow.isResultAvailable()) {
            InfoflowResults map = infoflow.getResults();
            Assert.assertEquals(resultCount, map.size());
//            assertTrue(primarySinks.stream().anyMatch(map::containsSinkMethod));
//            assertTrue(primarySinks.stream()
//                    .flatMap(sink -> sources.stream().map(source -> new Pair<>(sink, source)))
//                    .anyMatch(p -> map.isPathBetweenMethods(p.getO1(), p.getO2())));
        } else {
            Assert.fail("result is not available");
        }
    }

    protected void negativeCheckInfoflow(IInfoflow infoflow) {
        if (infoflow.isResultAvailable()) {
            InfoflowResults map = infoflow.getResults();
            Assert.assertEquals(0, map.size());
            Assert.assertTrue(primarySinks.stream().noneMatch(map::containsSinkMethod));
        }
    }

    @Test(timeout = 300000)
    public void testBufferedOutputStream1() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.integration.test.EvalTestCode: void testBufferedOutputStream1()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("test/java/soot/jimple/infoflow/integration/test/EvalRiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 1);
    }

    @Test(timeout = 300000)
    public void testBufferedOutputStream2() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.integration.test.EvalTestCode: void testBufferedOutputStream2()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("test/java/soot/jimple/infoflow/integration/test/EvalRiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 1);
    }

    @Test(timeout = 300000)
    public void testObjectOutputStream1() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.integration.test.EvalTestCode: void testObjectOutputStream1()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("test/java/soot/jimple/infoflow/integration/test/EvalRiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 1);
    }

    @Test(timeout = 300000)
    public void testObjectOutputStream2() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.integration.test.EvalTestCode: void testObjectOutputStream2()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("test/java/soot/jimple/infoflow/integration/test/EvalRiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 1);
    }
    @Test(timeout = 300000)
    public void testDataOutputStream1() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.integration.test.EvalTestCode: void testDataOutputStream1()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("test/java/soot/jimple/infoflow/integration/test/EvalRiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 1);
    }

    @Test(timeout = 300000)
    public void testDataOutputStream2() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.integration.test.EvalTestCode: void testDataOutputStream2()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("test/java/soot/jimple/infoflow/integration/test/EvalRiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 1);
    }

    @Test(timeout = 300000)
    public void testOutputStreamWriter1() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.integration.test.EvalTestCode: void testOutputStreamWriter1()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("test/java/soot/jimple/infoflow/integration/test/EvalRiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 1);
    }

    @Test(timeout = 300000)
    public void testOutputStreamWriter2() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.integration.test.EvalTestCode: void testOutputStreamWriter2()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("test/java/soot/jimple/infoflow/integration/test/EvalRiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 1);
    }

    @Test(timeout = 300000)
    public void testPrintWriter1() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.integration.test.EvalTestCode: void testPrintWriter1()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("test/java/soot/jimple/infoflow/integration/test/EvalRiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 1);
    }

    @Test(timeout = 300000)
    public void testPrintWriter2() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.integration.test.EvalTestCode: void testPrintWriter2()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("test/java/soot/jimple/infoflow/integration/test/EvalRiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 1);
    }
}
