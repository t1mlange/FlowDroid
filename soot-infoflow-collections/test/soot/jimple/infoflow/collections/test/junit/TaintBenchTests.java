package soot.jimple.infoflow.collections.test.junit;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.collections.StringResourcesResolver;
import soot.jimple.infoflow.collections.StubDroidBasedTaintWrapper;
import soot.jimple.infoflow.collections.parser.StubDroidSummaryProvider;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class TaintBenchTests extends FlowDroidTests {
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

    @Override
    protected ITaintPropagationWrapper getTaintWrapper() {
        try {
            StubDroidSummaryProvider sp = new StubDroidSummaryProvider(new File("stubdroidBased"));
            sp.loadAdditionalSummaries("summariesManual");
            return new StubDroidBasedTaintWrapper(sp);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @Override
    protected void setConfiguration(InfoflowConfiguration config) {

    }

    private static final String pathToAPKs = "/home/lange/Downloads/TaintBench(1)";

    @Test
    public void testBackflash() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "backflash.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("backflash.apk: " + results.getResultSet().size());
    }


    @Test
    public void testBeita_com_beita_contact() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "beita_com_beita_contact.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("beita_com_beita_contact.apk: " + results.getResultSet().size());
    }


    @Test
    public void testCajino_baidu() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "cajino_baidu.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("cajino_baidu.apk: " + results.getResultSet().size());
    }


    @Test
    public void testChat_hook() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "chat_hook.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("chat_hook.apk: " + results.getResultSet().size());
    }


    @Test
    public void testChulia() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "chulia.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("chulia.apk: " + results.getResultSet().size());
    }


    @Test
    public void testDeath_ring_materialflow() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "death_ring_materialflow.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("death_ring_materialflow.apk: " + results.getResultSet().size());
    }


    @Test
    public void testDsencrypt_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "dsencrypt_samp.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("dsencrypt_samp.apk: " + results.getResultSet().size());
    }


    @Test
    public void testExprespam() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "exprespam.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("exprespam.apk: " + results.getResultSet().size());
    }


    @Test
    public void testFakeappstore() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "fakeappstore.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("fakeappstore.apk: " + results.getResultSet().size());
    }


    @Test
    public void testFakebank_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "fakebank_android_samp.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("fakebank_android_samp.apk: " + results.getResultSet().size());
    }


    @Test
    public void testFakedaum() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "fakedaum.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("fakedaum.apk: " + results.getResultSet().size());
    }


    @Test
    public void testFakemart() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "fakemart.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("fakemart.apk: " + results.getResultSet().size());
    }


    @Test
    public void testFakeplay() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "fakeplay.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("fakeplay.apk: " + results.getResultSet().size());
    }


    @Test
    public void testFaketaobao() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "faketaobao.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("faketaobao.apk: " + results.getResultSet().size());
    }


    @Test
    public void testGodwon_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "godwon_samp.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("godwon_samp.apk: " + results.getResultSet().size());
    }


    @Test
    public void testHummingbad_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "hummingbad_android_samp.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("hummingbad_android_samp.apk: " + results.getResultSet().size());
    }


    @Test
    public void testJollyserv() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "jollyserv.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("jollyserv.apk: " + results.getResultSet().size());
    }


    @Test
    public void testOverlay_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "overlay_android_samp.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("overlay_android_samp.apk: " + results.getResultSet().size());
    }


    @Test
    public void testOverlaylocker2_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "overlaylocker2_android_samp.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("overlaylocker2_android_samp.apk: " + results.getResultSet().size());
    }


    @Test
    public void testPhospy() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "phospy.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("phospy.apk: " + results.getResultSet().size());
    }


    @Test
    public void testProxy_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "proxy_samp.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("proxy_samp.apk: " + results.getResultSet().size());
    }


    @Test
    public void testRemote_control_smack() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "remote_control_smack.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("remote_control_smack.apk: " + results.getResultSet().size());
    }


    @Test
    public void testRepane() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "repane.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("repane.apk: " + results.getResultSet().size());
    }


    @Test
    public void testRoidsec() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "roidsec.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("roidsec.apk: " + results.getResultSet().size());
    }


    @Test
    public void testSamsapo() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "samsapo.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("samsapo.apk: " + results.getResultSet().size());
    }


    @Test
    public void testSave_me() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "save_me.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("save_me.apk: " + results.getResultSet().size());
    }


    @Test
    public void testScipiex() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "scipiex.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("scipiex.apk: " + results.getResultSet().size());
    }


    @Test
    public void testSlocker_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "slocker_android_samp.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("slocker_android_samp.apk: " + results.getResultSet().size());
    }


    @Test
    public void testSms_google() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "sms_google.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("sms_google.apk: " + results.getResultSet().size());
    }


    @Test
    public void testSms_send_locker_qqmagic() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "sms_send_locker_qqmagic.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("sms_send_locker_qqmagic.apk: " + results.getResultSet().size());
    }


    @Test
    public void testSmssend_packageinstaller() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "smssend_packageInstaller.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("smssend_packageInstaller.apk: " + results.getResultSet().size());
    }


    @Test
    public void testSmssilience_fake_vertu() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "smssilience_fake_vertu.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("smssilience_fake_vertu.apk: " + results.getResultSet().size());
    }


    @Test
    public void testSmsstealer_kysn_assassincreed_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "smsstealer_kysn_assassincreed_android_samp.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("smsstealer_kysn_assassincreed_android_samp.apk: " + results.getResultSet().size());
    }


    @Test
    public void testStels_flashplayer_android_update() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "stels_flashplayer_android_update.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("stels_flashplayer_android_update.apk: " + results.getResultSet().size());
    }


    @Test
    public void testTetus() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "tetus.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("tetus.apk: " + results.getResultSet().size());
    }


    @Test
    public void testThe_interview_movieshow() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "the_interview_movieshow.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("the_interview_movieshow.apk: " + results.getResultSet().size());
    }


    @Test
    public void testThreatjapan_uracto() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "threatjapan_uracto.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("threatjapan_uracto.apk: " + results.getResultSet().size());
    }


    @Test
    public void testVibleaker_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "vibleaker_android_samp.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("vibleaker_android_samp.apk: " + results.getResultSet().size());
    }


    @Test
    public void testXbot_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "xbot_android_samp.apk");
        InfoflowResults results = app.runInfoflow("TB_SourcesAndSinks.txt");
        System.out.println("xbot_android_samp.apk: " + results.getResultSet().size());
    }
}
