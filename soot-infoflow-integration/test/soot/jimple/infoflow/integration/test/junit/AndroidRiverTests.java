package soot.jimple.infoflow.integration.test.junit;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.callbacks.codeql.CallbackEnricher;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointCreator;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.util.DebugFlowFunctionTaintPropagationHandler;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.toolkits.scalar.NopEliminator;
import soot.util.Chain;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

public class AndroidRiverTests extends RiverJUnitTests {
    @Override
    protected ITaintPropagationWrapper getTaintWrapper() {
        try {
            return TaintWrapperFactory.createTaintWrapper(Collections.singleton("../soot-infoflow-summaries/summariesManual"));
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException("Could not initialized Taintwrapper:");
        }
    }

    @BeforeClass
    public static void setUp() throws IOException {
        commonSetup();
    }

    @Test(timeout = 300000)
    public void conditionalTestApk() throws IOException {
        // The test apk has two java.io.OutputStream: void write(byte[]) sinks.
        // One is located in the KeepFlow activity and is a stream to the internet.
        // The other one is in the DiscardFlow activity and is a ByteArrayOutputStream.
        SetupApplication app = initApplication("testAPKs/ConditionalFlowTest.apk");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("./build/classes/res/AndroidRiverSourcesAndSinks.xml");
        InfoflowResults results = app.runInfoflow(parser);
        Assert.assertEquals(2, results.size());

        // Check that the flow is in the right activity
        SootMethod sm1 = Scene.v().grabMethod("<com.example.conditionalflowtestapp.KeepFlow: void onCreate(android.os.Bundle)>");
        SootMethod sm2 = Scene.v().grabMethod("<com.example.conditionalflowtestapp.KeepFlow: void leakToInternet(byte[])>");
        SootMethod sm3 = Scene.v().grabMethod("<com.example.conditionalflowtestapp.KeepFlow: void leakToExternalFile(byte[])>");
        Set<Unit> units = new HashSet<>();
        units.addAll(sm1.getActiveBody().getUnits());;
        units.addAll(sm2.getActiveBody().getUnits());;
        units.addAll(sm3.getActiveBody().getUnits());
        for (DataFlowResult result : results.getResultSet())
            Assert.assertTrue(Arrays.stream(result.getSource().getPath()).allMatch(units::contains));
    }

    @Test(timeout = 300000)
    public void externalFileWithNativeNameApk() throws IOException {
        // The test apk logs to an external file that is constructed with getExternalDir(null). The flow looks as follows:
        // path = getExternalDir(null).getAbsolutePath()
        // f = new File(path + jniCall);
        // FileWriter fw = new FileWriter(f);
        // BufferedWriter bw = new BufferedWriter(fw);
        // fw.append(tainted);
        SetupApplication app = initApplication("testAPKs/ExternalFileWithNativeName.apk");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("./build/classes/res/AndroidRiverSourcesAndSinks.xml");
        InfoflowResults results = app.runInfoflow(parser);
        Assert.assertEquals(1, results.size());
    }

    @Test(timeout = 300000)
    public void printWriterTestApk() throws IOException {
        // Also see OutputStreamTestCode#testPrintWriter3 but this time in Android
        // because Soot generates different jimple for Android and Java.
        SetupApplication app = initApplication("testAPKs/PrintWriterTest.apk");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("./build/classes/res/AndroidRiverSourcesAndSinks.xml");
        InfoflowResults results = app.runInfoflow(parser);
        Assert.assertEquals(1, results.size());
    }

    @Test//(timeout = 300000)
    public void adb() throws IOException {
        String androidJars = System.getenv("ANDROID_JARS");
        if (androidJars == null)
            androidJars = System.getProperty("ANDROID_JARS");
        if (androidJars == null)
            throw new RuntimeException("Android JAR dir not set");
        System.out.println("Loading Android.jar files from " + androidJars);

//        String apk = "/home/lange/paper-remote/paper-conditionalflows/fdroid-sources/apps/Twire/Twire/app/build/outputs/apk/debug/Twire-2.10.8-DEBUG.apk";
        String apk = "/home/lange/paper-remote/paper-conditionalflows/fdroid-sources/apps/seadroid/seadroid/app/build/outputs/apk/debug/seafile-debug-2.3.5.apk";
//        String apk = "/home/lange/paper-remote/paper-conditionalflows/fdroid-results2/Infinity-For-Reddit/InfinityForReddit-debug.apk";
        SetupApplication app = new SetupApplication(androidJars, apk) {
            protected AndroidEntryPointCreator createEntryPointCreator(Set<SootClass> components) {
                File f = new File("unique_packages.txt");
                if (!f.exists())
                    throw new RuntimeException("WHERE IS UNIQUE_PACKAGES, I'm here: " + new File("").getAbsolutePath());
                List<String> lines;
                try {
                    lines = Files.readAllLines(f.getAbsoluteFile().toPath(), Charset.defaultCharset());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                lines.add("com.example.");

                Set<String> seeds =  new HashSet<>();
                for (SootClass sc : new HashSet<>(Scene.v().getApplicationClasses())) {
                    if (lines.stream().noneMatch(line -> sc.getName().startsWith(line))) {
                        sc.setLibraryClass();
                        continue;
                    }

                    for (SootMethod sm : sc.getMethods()) {
                        if (!sm.isConcrete())
                            continue;
                        seeds.add(sm.getSignature());
                    }
                }

                return new AndroidEntryPointCreator(manifest, Collections.emptySet()) {
                    @Override
                    protected SootMethod createDummyMainInternal() {
                        Map<String, Set<String>> classMap = SootMethodRepresentationParser.v().parseClassNames(seeds, false);

                        // create new class:
                        HashMap<String, Local> localVarsForClasses = new HashMap<String, Local>();

                        // create constructors:
                        for (String className : classMap.keySet()) {
                            SootClass createdClass = Scene.v().forceResolve(className, SootClass.BODIES);
                            createdClass.setApplicationClass();

                            Local localVal = generateClassConstructor(createdClass);
                            if (localVal == null) {
                                continue;
                            }
                            localVarsForClasses.put(className, localVal);
                        }

                        // add entrypoint calls
                        int conditionCounter = 0;
                        final Jimple jimple = Jimple.v();
                        NopStmt startStmt = jimple.newNopStmt();
                        NopStmt endStmt = jimple.newNopStmt();
                        Value intCounter = generator.generateLocal(IntType.v());
                        body.getUnits().add(startStmt);
                        for (Map.Entry<String, Set<String>> entry : classMap.entrySet()) {
                            Local classLocal = localVarsForClasses.get(entry.getKey());
                            for (String method : entry.getValue()) {
                                SootMethodAndClass methodAndClass = SootMethodRepresentationParser.v().parseSootMethodString(method);
                                SootMethod currentMethod = findMethod(Scene.v().getSootClass(methodAndClass.getClassName()),
                                        methodAndClass.getSubSignature());
                                if (currentMethod == null) {
                                    continue;
                                }

                                EqExpr cond = jimple.newEqExpr(intCounter, IntConstant.v(conditionCounter));
                                conditionCounter++;
                                NopStmt thenStmt = jimple.newNopStmt();
                                IfStmt ifStmt = jimple.newIfStmt(cond, thenStmt);
                                body.getUnits().add(ifStmt);
                                buildMethodCall(currentMethod, classLocal);
                                body.getUnits().add(thenStmt);
                            }
                        }
                        body.getUnits().add(endStmt);
                        GotoStmt gotoStart = jimple.newGotoStmt(startStmt);
                        body.getUnits().add(gotoStart);

                        body.getUnits().add(Jimple.v().newReturnVoidStmt());
                        NopEliminator.v().transform(body);
                        eliminateSelfLoops();
                        return mainMethod;
                    }
                };
            }
        };

        app.getConfig().setMergeDexFiles(true);
        app.setTaintWrapper(getTaintWrapper());
        app.getConfig().setAdditionalFlowsEnabled(true);
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("./build/classes/res/AndroidRiverSourcesAndSinks.xml");
//        app.addPreprocessor(new CallbackEnricher(app.getConfig().getAnalysisFileConfig().getTargetAPKFile()));
        app.getConfig().setExcludeSootLibraryClasses(true);
//        app.setTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
//        app.getConfig().setWriteOutputFiles(true);
        app.getConfig().setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.CHA);
        InfoflowResults results = app.runInfoflow(parser);
        Assert.assertEquals(1, results.size());
    }


    @Test
    public void externalCacheDirTest() throws IOException {
        // Test flow with getExternalCacheDir wrapped in another File constructor
        SetupApplication app = initApplication("/home/lange/apps/ExternalCacheDirTest.apk");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("./build/classes/res/AndroidRiverSourcesAndSinks.xml");
        InfoflowResults results = app.runInfoflow(parser);
        Assert.assertEquals(1, results.size());
    }

}
