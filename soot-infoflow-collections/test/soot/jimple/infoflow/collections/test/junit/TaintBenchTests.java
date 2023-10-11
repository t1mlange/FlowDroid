package soot.jimple.infoflow.collections.test.junit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.collections.CollectionsSetupApplication;
import soot.jimple.infoflow.collections.StubDroidBasedTaintWrapper;
import soot.jimple.infoflow.collections.parser.StubDroidSummaryProvider;
import soot.jimple.infoflow.collections.strategies.containers.ConstantMapStrategy;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.util.DebugFlowFunctionTaintPropagationHandler;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TaintBenchTests extends FlowDroidTests {
    public static class ExpectedResult {
        public final String source;
        public final String sourceClass;
        public final String sourceCaller;
        public final String sink;
        public final String sinkClass;
        public final String sinkCaller;

        public ExpectedResult(String source, String sourceClass, String sourceCaller,
                              String sink, String sinkClass, String sinkCaller) {
            this.source = source;
            this.sourceClass = sourceClass;
            this.sourceCaller = sourceCaller;

            this.sink = sink;
            this.sinkClass = sinkClass;
            this.sinkCaller = sinkCaller;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\t").append(source).append(" in <").append(sourceClass).append(": _ ").append(sourceCaller).append("(...)>\n");
            sb.append("\t").append(sink).append(" in <").append(sinkClass).append(": _ ").append(sinkCaller).append("(...)>");
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExpectedResult that = (ExpectedResult) o;
            return Objects.equals(source, that.source) && Objects.equals(sourceClass, that.sourceClass)
                    && Objects.equals(sourceCaller, that.sourceCaller) && Objects.equals(sink, that.sink)
                    && Objects.equals(sinkClass, that.sinkClass) && Objects.equals(sinkCaller, that.sinkCaller);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, sourceClass, sourceCaller, sink, sinkClass, sinkCaller);
        }
    }

    private static final MultiMap<String, String> ssMap = new HashMultiMap<>();
    static {
        ssMap.put("beita_com_beita_contact.apk", "<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)> -> _SOURCE_");
        ssMap.put("beita_com_beita_contact.apk", "<java.io.BufferedWriter: void write(java.lang.String)> -> _SINK_");
        ssMap.put("beita_com_beita_contact.apk", "<javax.mail.Transport: void sendMessage(javax.mail.Message,javax.mail.Address[])> -> _SINK_");
        ssMap.put("chulia.apk", "<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)> -> _SOURCE_");
        ssMap.put("chulia.apk", "<android.location.Location: double getLongitude()> -> _SOURCE_");
        ssMap.put("chulia.apk", "<com.google.services.AlarmService: void sendBroadcast(android.content.Intent)> -> _SINK_");
        // no traditional sink but needed because ICC
//        ssMap.put("chulia.apk", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)> -> _SINK_");
        ssMap.put("death_ring_materialflow.apk", "<android.telephony.gsm.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)> -> _SINK_");
        ssMap.put("death_ring_materialflow.apk", "<android.telephony.gsm.SmsMessage: java.lang.String getDisplayMessageBody()> -> _SOURCE_");
        ssMap.put("exprespam.apk", "<frhfsd.siksdk.ujdsfjkfsd.WrehifsdkjsActivity: android.database.Cursor managedQuery(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)> -> _SOURCE_");
        ssMap.put("exprespam.apk", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)> -> _SINK_");
        ssMap.put("fakeappstore.apk", "<android.telephony.TelephonyManager: java.lang.String getDeviceId()> -> _SOURCE_");
        ssMap.put("fakeappstore.apk", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)> -> _SINK_");
        ssMap.put("fakebank_android_samp.apk", "<android.telephony.SmsMessage: java.lang.String getDisplayMessageBody()> -> _SOURCE_");
        ssMap.put("fakebank_android_samp.apk", "<android.telephony.TelephonyManager: java.lang.String getSimSerialNumber()> -> _SOURCE_");
        ssMap.put("fakebank_android_samp.apk", "<java.net.HttpURLConnection: java.io.InputStream getInputStream()> -> _SINK_");
        ssMap.put("fakebank_android_samp.apk", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)> -> _SINK_");
        ssMap.put("fakebank_android_samp.apk", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)> -> _SINK_");
        ssMap.put("fakedaum.apk", "<android.telephony.SmsMessage: android.telephony.SmsMessage createFromPdu(byte[])> -> _SOURCE_");
        ssMap.put("fakedaum.apk", "<android.telephony.TelephonyManager: java.lang.String getSimSerialNumber()> -> _SOURCE_");
        ssMap.put("fakedaum.apk", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)> -> _SINK_");
        ssMap.put("faketaobao.apk", "<android.widget.EditText: android.text.Editable getText()> -> _SOURCE_");
        ssMap.put("faketaobao.apk", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)> -> _SINK_");
        ssMap.put("godwon_samp.apk", "<android.content.Intent: java.io.Serializable getSerializableExtra(java.lang.String)> -> _SOURCE_");
        ssMap.put("godwon_samp.apk", "<android.telephony.TelephonyManager: java.lang.String getDeviceId()> -> _SOURCE_");
        ssMap.put("godwon_samp.apk", "<android.telephony.TelephonyManager: java.lang.String getLine1Number()> -> _SOURCE_");
        ssMap.put("godwon_samp.apk", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)> -> _SINK_");
        ssMap.put("overlay_android_samp.apk", "<android.content.Context: android.content.ComponentName startService(android.content.Intent)> -> _SINK_");
        ssMap.put("overlay_android_samp.apk", "<android.content.Intent: android.os.Bundle getExtras()> -> _SOURCE_");
        ssMap.put("proxy_samp.apk", "<android.telephony.gsm.GsmCellLocation: int getLac()> -> _SOURCE_");
        ssMap.put("proxy_samp.apk", "<java.io.File: void <init>(java.io.File,java.lang.String)> -> _SOURCE_");
        ssMap.put("proxy_samp.apk", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)> -> _SINK_");
        ssMap.put("proxy_samp.apk", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)> -> _SINK_");
        ssMap.put("roidsec.apk", "<android.content.pm.PackageManager: java.util.List getInstalledPackages(int)> -> _SOURCE_");
        ssMap.put("roidsec.apk", "<java.io.OutputStream: void write(byte[])> -> _SINK_");
        ssMap.put("samsapo.apk", "<android.database.Cursor: java.lang.String getString(int)> -> _SOURCE_");
        ssMap.put("samsapo.apk", "<android.telephony.gsm.SmsMessage: android.telephony.gsm.SmsMessage createFromPdu(byte[])> -> _SOURCE_");
        ssMap.put("samsapo.apk", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)> -> _SINK_");
        ssMap.put("save_me.apk", "<android.database.sqlite.SQLiteDatabase: android.database.Cursor query(java.lang.String,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String,java.lang.String,java.lang.String)> -> _SOURCE_");
        ssMap.put("save_me.apk", "<android.net.wifi.WifiInfo: java.lang.String getMacAddress()> -> _SOURCE_");
        ssMap.put("save_me.apk", "<android.telephony.TelephonyManager: java.lang.String getSimCountryIso()> -> _SOURCE_");
        ssMap.put("save_me.apk", "<android.widget.EditText: android.text.Editable getText()> -> _SOURCE_");
        ssMap.put("save_me.apk", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)> -> _SINK_");
        ssMap.put("scipiex.apk", "<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)> -> _SOURCE_");
        ssMap.put("scipiex.apk", "<android.telephony.SmsMessage: android.telephony.SmsMessage createFromPdu(byte[])> -> _SOURCE_");
        ssMap.put("scipiex.apk", "<android.telephony.TelephonyManager: java.lang.String getLine1Number()> -> _SOURCE_");
        ssMap.put("scipiex.apk", "<java.io.OutputStream: void write(byte[])> -> _SINK_");
        ssMap.put("scipiex.apk", "<java.io.PrintWriter: void println(java.lang.String)> -> _SINK_");
        ssMap.put("slocker_android_samp.apk", "<android.telephony.TelephonyManager: java.lang.String getDeviceId()> -> _SOURCE_");
        ssMap.put("slocker_android_samp.apk", "<android.widget.EditText: android.text.Editable getText()> -> _SOURCE_");
        ssMap.put("slocker_android_samp.apk", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)> -> _SINK_");
        ssMap.put("sms_google.apk", "<com.google.elements.Utils: java.lang.String getDeviceId()> -> _SOURCE_");
        ssMap.put("sms_google.apk", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)> -> _SINK_");
        ssMap.put("stels_flashplayer_android_update.apk", "<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)> -> _SOURCE_");
        ssMap.put("stels_flashplayer_android_update.apk", "<android.content.pm.PackageManager: java.util.List getInstalledPackages(int)> -> _SOURCE_");
        ssMap.put("stels_flashplayer_android_update.apk", "<android.telephony.TelephonyManager: java.lang.String getSubscriberId()> -> _SOURCE_");
        ssMap.put("stels_flashplayer_android_update.apk", "<java.io.DataOutputStream: void write(byte[])> -> _SINK_");
//        ssMap.put("vibleaker_android_samp.apk", "<android.os.Environment: java.io.File getExternalStorageDirectory()> -> _SOURCE_");
        ssMap.put("vibleaker_android_samp.apk", "<java.io.File: void <init>(java.lang.String)> -> _SOURCE_");
        ssMap.put("vibleaker_android_samp.apk", "<org.springframework.web.client.RestTemplate: org.springframework.http.ResponseEntity exchange(java.lang.String,org.springframework.http.HttpMethod,org.springframework.http.HttpEntity,java.lang.Class,java.lang.Object[])> -> _SINK_");
        ssMap.put("xbot_android_samp.apk", "<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)> -> _SOURCE_");
        ssMap.put("xbot_android_samp.apk", "<android.webkit.WebView: void addJavascriptInterface(java.lang.Object,java.lang.String)> -> _SINK_");
    }

    private static final MultiMap<String, ExpectedResult> resultMap = new HashMultiMap<>();
    static {
        // CG problem?
//        resultMap.put("beita_com_beita_contact.apk", new ExpectedResult("<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)>", "com.beita.contact.MyContacts", "getContactsInfoListFromPhone", "<java.io.BufferedWriter: void write(java.lang.String)>", "com.beita.contact.ContactUtil", "write"));
//        resultMap.put("beita_com_beita_contact.apk", new ExpectedResult("<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)>", "com.beita.contact.MyContacts", "getContactsInfoListFromPhone", "<javax.mail.Transport: void sendMessage(javax.mail.Message,javax.mail.Address[])>", "com.beita.contact.MailUtil", "sendByJavaMail"));
        // Adapted for non-ICC flows
        resultMap.put("chulia.apk", new ExpectedResult("<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)>", "com.google.services.AlarmService", "getPhoneContacts", "<com.google.services.AlarmService: void sendBroadcast(android.content.Intent)>", "com.google.services.AlarmService", "onCreate"));
        resultMap.put("chulia.apk", new ExpectedResult("<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)>", "com.google.services.AlarmService", "getRecord", "<com.google.services.AlarmService: void sendBroadcast(android.content.Intent)>", "com.google.services.AlarmService", "onCreate"));
        resultMap.put("chulia.apk", new ExpectedResult("<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)>", "com.google.services.AlarmService", "getSms", "<com.google.services.AlarmService: void sendBroadcast(android.content.Intent)>", "com.google.services.AlarmService", "onStart"));
        resultMap.put("chulia.apk", new ExpectedResult("<android.location.Location: double getLongitude()>", "com.google.services.AlarmService", "updateWithNewLocation", "<com.google.services.AlarmService: void sendBroadcast(android.content.Intent)>", "com.google.services.AlarmService", "updateWithNewLocation"));
        resultMap.put("death_ring_materialflow.apk", new ExpectedResult("<android.telephony.gsm.SmsMessage: java.lang.String getDisplayMessageBody()>", "com.qc.access.SmsReceiver", "onReceive", "<android.telephony.gsm.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>", "com.qc.model.SmsSenderAndReceiver", "send2"));
        resultMap.put("exprespam.apk", new ExpectedResult("<frhfsd.siksdk.ujdsfjkfsd.WrehifsdkjsActivity: android.database.Cursor managedQuery(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)>", "frhfsd.siksdk.ujdsfjkfsd.WrehifsdkjsActivity.Progress", "getAddress", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "frhfsd.siksdk.ujdsfjkfsd.WrehifsdkjsActivity.Progress", "doPost"));
        resultMap.put("fakeappstore.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getDeviceId()>", "com.google.games.stores.util.GeneralUtil", "getDevice", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.google.games.stores.util.NetTask", "doInBackground"));
        resultMap.put("fakebank_android_samp.apk", new ExpectedResult("<android.telephony.SmsMessage: java.lang.String getDisplayMessageBody()>", "com.example.smsmanager.smsReceiver", "onReceive", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.example.smsmanager.smsReceiver$1", "run"));
        resultMap.put("fakebank_android_samp.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getSimSerialNumber()>", "com.example.bankmanager.BankEndActivity.CreateNewUser", "doInBackground", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "cn.smsmanager.tools.JSONParser", "makeHttpRequest"));
        resultMap.put("fakebank_android_samp.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getSimSerialNumber()>", "com.example.smsmanager.BootCompleteBroadcastReceiver", "onReceive", "<java.net.HttpURLConnection: java.io.InputStream getInputStream()>", "cn.smsmanager.internet.HttpRequest", "sendGetRequest"));
        // ICC
//        resultMap.put("fakebank_android_samp.apk", new ExpectedResult("<com.example.bankmanager.BankActivity: android.view.View findViewById(int)>", "com.example.bankmanager.BankActivity", "onCreate", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "cn.smsmanager.tools.JSONParser", "makeHttpRequest"));
        // Callgraph missing startService -> onStart edge?!?
//        resultMap.put("fakedaum.apk", new ExpectedResult("<android.telephony.SmsMessage: android.telephony.SmsMessage createFromPdu(byte[])>", "com.mvlove.receiver.SmsReceiver", "onReceive", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.mvlove.http.HttpWrapper", "post"));
        resultMap.put("fakedaum.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getSimSerialNumber()>", "com.mvlove.util.PhoneUtil", "getImei", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.mvlove.http.HttpWrapper", "post"));
        resultMap.put("faketaobao.apk", new ExpectedResult("<android.widget.EditText: android.text.Editable getText()>", "com.tao.bao.LocationVerify$1", "onClick", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.tao.bao.ToolHelper", "postData"));
        resultMap.put("faketaobao.apk", new ExpectedResult("<android.widget.EditText: android.text.Editable getText()>", "com.tao.bao.MainActivity$1", "onClick", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.tao.bao.ToolHelper", "postData"));
        resultMap.put("godwon_samp.apk", new ExpectedResult("<android.content.Intent: java.io.Serializable getSerializableExtra(java.lang.String)>", "android.sms.core.BootReceiver", "SmsMessage[] getMessagesFromIntent", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "android.sms.core.ToolHelper", "postData"));
        resultMap.put("godwon_samp.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getDeviceId()>", "android.sms.core.BootReceiver", "onReceive", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "android.sms.core.ToolHelper", "postData"));
        resultMap.put("godwon_samp.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getDeviceId()>", "android.sms.core.GoogleService", "onCreate", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "android.sms.core.ToolHelper", "postData"));
        resultMap.put("godwon_samp.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getLine1Number()>", "android.sms.core.BootReceiver", "onReceive", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "android.sms.core.ToolHelper", "postData"));
        resultMap.put("godwon_samp.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getLine1Number()>", "android.sms.core.GoogleService", "onCreate", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "android.sms.core.ToolHelper", "postData"));
        resultMap.put("overlay_android_samp.apk", new ExpectedResult("<android.content.Intent: android.os.Bundle getExtras()>", "exts.whats.MessageReceiver", "retrieveMessages", "<android.content.Context: android.content.ComponentName startService(android.content.Intent)>", "exts.whats.MessageReceiver", "onReceive"));
        resultMap.put("proxy_samp.apk", new ExpectedResult("<android.telephony.gsm.GsmCellLocation: int getLac()>", "com.smart.studio.proxy.ProxyService.PollerThread", "run", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.smart.studio.proxy.ProxyService.PollerThread", "run"));
        resultMap.put("proxy_samp.apk", new ExpectedResult("<java.io.File: void <init>(java.io.File,java.lang.String)>", "com.smart.studio.proxy.ProxyService.ProxyThread", "run", "<org.apache.http.impl.client.DefaultHttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.smart.studio.proxy.ProxyService.ProxyThread", "run"));
        resultMap.put("roidsec.apk", new ExpectedResult("<android.content.pm.PackageManager: java.util.List getInstalledPackages(int)>", "cn.phoneSync.PhoneSyncService", "getKernelApp", "<java.io.OutputStream: void write(byte[])>", "cn.phoneSync.PhoneSyncService", "BackConnTask"));
        resultMap.put("samsapo.apk", new ExpectedResult("<android.database.Cursor: java.lang.String getString(int)>", "com.android.tools.system.SplashScreen", "getMessages", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.android.tools.system.MyPostRequest", "doInBackground"));
        // Trash flow, contained in other flow, but marking execute as sink excludes the other flow....
        // resultMap.put("samsapo.apk", new ExpectedResult("<android.telephony.gsm.SmsMessage: android.telephony.gsm.SmsMessage createFromPdu(byte[])>", "com.android.tools.system.SmsReceiver", "onReceive", "<com.android.tools.system.MyPostRequest: android.os.AsyncTask execute(java.lang.Object[])>", "com.android.tools.system.SmsReceiver", "onReceive"));
        resultMap.put("samsapo.apk", new ExpectedResult("<android.telephony.gsm.SmsMessage: android.telephony.gsm.SmsMessage createFromPdu(byte[])>", "com.android.tools.system.SmsReceiver", "onReceive", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.android.tools.system.MyPostRequest", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.database.sqlite.SQLiteDatabase: android.database.Cursor query(java.lang.String,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String,java.lang.String,java.lang.String)>", "com.savemebeta.DatabaseOperations", "getInformation", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.CO.sendcontact", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.database.sqlite.SQLiteDatabase: android.database.Cursor query(java.lang.String,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String,java.lang.String,java.lang.String)>", "com.savemebeta.DatabaseOperationssmssave", "getInformation", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.Scan.sendsmsdata", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.database.sqlite.SQLiteDatabase: android.database.Cursor query(java.lang.String,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String,java.lang.String,java.lang.String)>", "com.savemebeta.DatabaseOperationssmssave", "getInformation", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.Scan.sendsmsdata2", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.database.sqlite.SQLiteDatabase: android.database.Cursor query(java.lang.String,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String,java.lang.String,java.lang.String)>", "com.savemebeta.DatabaseOperationssmssave2", "getInformation", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.Scan.sendsmsdata2", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.net.wifi.WifiInfo: java.lang.String getMacAddress()>", "com.savemebeta.CHECKUPD", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.CHECKUPD.senddata", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.net.wifi.WifiInfo: java.lang.String getMacAddress()>", "com.savemebeta.CHECKUPD", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.CHECKUPD.sendmyinfos", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.net.wifi.WifiInfo: java.lang.String getMacAddress()>", "com.savemebeta.CHECKUPD", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.CHECKUPD.sendmystatus", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.net.wifi.WifiInfo: java.lang.String getMacAddress()>", "com.savemebeta.CO", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.CO.sendcontact", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.net.wifi.WifiInfo: java.lang.String getMacAddress()>", "com.savemebeta.CO", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.update.sendmystatus", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.net.wifi.WifiInfo: java.lang.String getMacAddress()>", "com.savemebeta.GTSTSR", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.GTSTSR.StatusTask", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.net.wifi.WifiInfo: java.lang.String getMacAddress()>", "com.savemebeta.GTSTSR", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.GTSTSR.globaldata", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.net.wifi.WifiInfo: java.lang.String getMacAddress()>", "com.savemebeta.GTSTSR", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.update.sendmystatus", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.net.wifi.WifiInfo: java.lang.String getMacAddress()>", "com.savemebeta.RC", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.RC.StatusTask", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getSimCountryIso()>", "com.savemebeta.CHECKUPD", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.CHECKUPD.sendmyinfos", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getSimCountryIso()>", "com.savemebeta.CO", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.CO.sendcontact", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.widget.EditText: android.text.Editable getText()>", "com.savemebeta.Analyse$1", "onClick", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.Scan.sendsmsdata", "doInBackground"));
        resultMap.put("save_me.apk", new ExpectedResult("<android.widget.EditText: android.text.Editable getText()>", "com.savemebeta.SOSsm$1", "onClick", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.savemebeta.SOSsm.sendsmsdata2", "doInBackground"));
        // CG problem: unsoundness of SPARK
        // resultMap.put("scipiex.apk", new ExpectedResult("<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)>", "com.yxx.jiejie.SMSListenerService", "readAllContacts", "<java.io.PrintWriter: void println(java.lang.String)>", "com.yxx.jiejie.SMSListenerService$2$1", "writeToFile"));
        // Not reachable because ICC
        // resultMap.put("scipiex.apk", new ExpectedResult("<android.telephony.SmsMessage: android.telephony.SmsMessage createFromPdu(byte[])>", "com.yxx.jiejie.SMSListenerService$1", "onReceive", "<java.io.OutputStream: void write(byte[])>", "com.yxx.jiejie.SendThread", "sendPostRequest"));
        // ICC
        // resultMap.put("scipiex.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getLine1Number()>", "com.yxx.jiejie.MainActivity", "onCreate", "<java.io.OutputStream: void write(byte[])>", "com.yxx.jiejie.SendThread", "sendPostRequest"));
        resultMap.put("slocker_android_samp.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getDeviceId()>", "com.android.locker.SenderActivity", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.android.locker.RequestSender", "checkState"));
        // Flows with static field, CG seems to be wrong
//        resultMap.put("slocker_android_samp.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getDeviceId()>", "com.android.locker.SenderActivity", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.android.locker.RequestSender", "sendCode"));
//        resultMap.put("slocker_android_samp.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getDeviceId()>", "com.android.locker.SenderActivity", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.android.locker.SenderActivity", "debug"));
        resultMap.put("slocker_android_samp.apk", new ExpectedResult("<android.widget.EditText: android.text.Editable getText()>", "com.android.locker.SenderActivity$12", "onClick", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.android.locker.RequestSender", "sendCode"));
        resultMap.put("slocker_android_samp.apk", new ExpectedResult("<android.widget.EditText: android.text.Editable getText()>", "com.android.locker.SenderActivity$12", "onClick", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.android.locker.SenderActivity", "debug"));
        resultMap.put("sms_google.apk", new ExpectedResult("<com.google.elements.Utils: java.lang.String getDeviceId()>", "com.google.elements.WorkService$1$1", "run", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.google.elements.Utils.sendPostRequest", "doInBackground"));
        resultMap.put("sms_google.apk", new ExpectedResult("<com.google.elements.Utils: java.lang.String getDeviceId()>", "com.google.elements.WorkService", "onCreate", "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", "com.google.elements.Utils.sendPostRequest", "doInBackground"));
        resultMap.put("stels_flashplayer_android_update.apk", new ExpectedResult("<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)>", "ru.stels2.Functions", "getContacts", "<java.io.DataOutputStream: void write(byte[])>", "ru.stels2.Functions", "sendHttpRequest"));
        resultMap.put("stels_flashplayer_android_update.apk", new ExpectedResult("<android.content.pm.PackageManager: java.util.List getInstalledPackages(int)>", "ru.stels2.Functions", "getInstalledAppList", "<java.io.DataOutputStream: void write(byte[])>", "ru.stels2.Functions", "sendHttpRequest"));
        // Complex flow, dunno whats wrong :(
//        resultMap.put("stels_flashplayer_android_update.apk", new ExpectedResult("<android.telephony.TelephonyManager: java.lang.String getSubscriberId()>", "ru.stels2.Functions", "String getImsi", "<java.io.DataOutputStream: void write(byte[])>", "ru.stels2.Functions", "sendHttpRequest"));
        // Path Building Problem because flows share much of the flow
//        resultMap.put("vibleaker_android_samp.apk", new ExpectedResult("<android.os.Environment: java.io.File getExternalStorageDirectory()>", "gr.georkouk.kastorakiacounter_new.MyServerFunctions", "register", "<org.springframework.web.client.RestTemplate: org.springframework.http.ResponseEntity exchange(java.lang.String,org.springframework.http.HttpMethod,org.springframework.http.HttpEntity,java.lang.Class,java.lang.Object[])>", "gr.georkouk.kastorakiacounter_new.MyServerFunctions", "upPst"));
        resultMap.put("vibleaker_android_samp.apk", new ExpectedResult("<java.io.File: void <init>(java.lang.String)>", "gr.georkouk.kastorakiacounter_new.MyServerFunctions", "upFF", "<org.springframework.web.client.RestTemplate: org.springframework.http.ResponseEntity exchange(java.lang.String,org.springframework.http.HttpMethod,org.springframework.http.HttpEntity,java.lang.Class,java.lang.Object[])>", "gr.georkouk.kastorakiacounter_new.MyServerFunctions", "upPst"));
        resultMap.put("xbot_android_samp.apk", new ExpectedResult("<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)>", "com.address.core.xAPI", "getContacts", "<android.webkit.WebView: void addJavascriptInterface(java.lang.Object,java.lang.String)>", "com.address.core.activities.BrowserActivity", "onCreate"));
    }

    protected SetupApplication initApplication(String fileName) {
        String androidJars = System.getenv("ANDROID_JARS");
        if (androidJars == null)
            androidJars = System.getProperty("ANDROID_JARS");
        if (androidJars == null)
            throw new RuntimeException("Android JAR dir not set");

        SetupApplication setupApplication = new CollectionsSetupApplication(androidJars, fileName);
        setupApplication.getConfig().setMergeDexFiles(true);
        setupApplication.setTaintWrapper(getTaintWrapper());
        setConfiguration(setupApplication.getConfig());

        return setupApplication;
    }

    @Override
    protected ITaintPropagationWrapper getTaintWrapper() {
//        try {
//            return TaintWrapperFactory.createTaintWrapperEager();
//        } catch (Exception e) {
//            throw new RuntimeException();
//        }
        try {
            StubDroidSummaryProvider sp = new StubDroidSummaryProvider(new File("stubdroidBased"));
            sp.loadAdditionalSummaries("summariesManual");
            return new StubDroidBasedTaintWrapper(sp, ConstantMapStrategy::new);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private static void compareResults(String apk, IInfoflowCFG cfg, InfoflowResults infoflowResults) {
        Set<ExpectedResult> expectedResults = resultMap.get(apk);
        if (expectedResults == null || expectedResults.isEmpty())
            return;

        boolean failed = false;
        ArrayList<String> fails = new ArrayList<>();
        for (ExpectedResult expectedResult : expectedResults) {
            boolean match = false;
            if (infoflowResults != null) {
                for (DataFlowResult res : infoflowResults.getResultSet()) {
                    Stmt sourceStmt = res.getSource().getStmt();
                    if (!sourceStmt.toString().contains(expectedResult.source))
                        continue;
                    SootMethod sourceMethod = cfg.getMethodOf(sourceStmt);
                    if (!sourceMethod.getName().equals(expectedResult.sourceCaller))
                        continue;

                    Stmt sinkStmt = res.getSink().getStmt();
                    if (!sinkStmt.toString().contains(expectedResult.sink))
                        continue;
                    SootMethod sinkMethod = cfg.getMethodOf(sinkStmt);
                    if (!sinkMethod.getName().equals(expectedResult.sinkCaller))
                        continue;

                    match = true;
                }

                if (!match) {
                    failed = true;
                    fails.add("Missing flow" + "\n" + expectedResult);
                }
            }
        }
        fails.forEach(System.out::println);
        Assert.assertFalse(failed);
    }

    private static PermissionMethodParser getSourcesAndSinks(String apk) throws IOException {
        return PermissionMethodParser.fromStringList(new ArrayList<>(ssMap.get(apk)));
    }

    @Override
    protected void setConfiguration(InfoflowConfiguration config) {

    }

    private static final String pathToAPKs = "/home/lange/Downloads/TaintBench(1)";

    @Test
    public void testBackflash() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "backflash.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("backflash.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("backflash.apk"));
    }


    @Test
    public void testBeita_com_beita_contact() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "beita_com_beita_contact.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("beita_com_beita_contact.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("beita_com_beita_contact.apk"));
        Assert.assertEquals(0, results.getResultSet().size());

    }


    @Test
    public void testCajino_baidu() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "cajino_baidu.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("cajino_baidu.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("cajino_baidu.apk"));
        Assert.assertEquals(0, results.getResultSet().size());
    }


    @Test
    public void testChat_hook() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "chat_hook.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("chat_hook.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("chat_hook.apk"));
        Assert.assertEquals(0, results.getResultSet().size());
    }


    @Test
    public void testChulia() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "chulia.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("chulia.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("chulia.apk"));
    }


    @Test
    public void testDeath_ring_materialflow() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "death_ring_materialflow.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("death_ring_materialflow.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("death_ring_materialflow.apk"));
    }


    @Test
    public void testDsencrypt_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "dsencrypt_samp.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("dsencrypt_samp.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("dsencrypt_samp.apk"));
        Assert.assertEquals(0, results.getResultSet().size());

    }


    @Test
    public void testExprespam() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "exprespam.apk");
        app.getConfig().setWriteOutputFiles(true);
        app.addResultsAvailableHandler((cfg, results) -> compareResults("exprespam.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("exprespam.apk"));
    }


    @Test
    public void testFakeappstore() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "fakeappstore.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("fakeappstore.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("fakeappstore.apk"));
    }


    @Test
    public void testFakebank_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "fakebank_android_samp.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("fakebank_android_samp.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("fakebank_android_samp.apk"));
    }


    @Test
    public void testFakedaum() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "fakedaum.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("fakedaum.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("fakedaum.apk"));
    }


    @Test
    public void testFakemart() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "fakemart.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("fakemart.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("fakemart.apk"));
    }


    @Test
    public void testFakeplay() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "fakeplay.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("fakeplay.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("fakeplay.apk"));
    }


    @Test
    public void testFaketaobao() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "faketaobao.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("faketaobao.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("faketaobao.apk"));
    }


    @Ignore("Soot problem or bullshit ground-truth")
    @Test
    public void testGodwon_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "godwon_samp.apk");
        app.getConfig().setWriteOutputFiles(true);
        app.addResultsAvailableHandler((cfg, results) -> compareResults("godwon_samp.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("godwon_samp.apk"));
    }


    @Test
    public void testHummingbad_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "hummingbad_android_samp.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("hummingbad_android_samp.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("hummingbad_android_samp.apk"));
        Assert.assertEquals(0, results.getResultSet().size());
    }


    @Test
    public void testJollyserv() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "jollyserv.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("jollyserv.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("jollyserv.apk"));
        Assert.assertEquals(0, results.getResultSet().size());
    }


    @Test
    public void testOverlay_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "overlay_android_samp.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("overlay_android_samp.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("overlay_android_samp.apk"));
    }


    @Test
    public void testOverlaylocker2_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "overlaylocker2_android_samp.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("overlaylocker2_android_samp.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("overlaylocker2_android_samp.apk"));
        Assert.assertEquals(0, results.getResultSet().size());
    }


    @Test
    public void testPhospy() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "phospy.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("phospy.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("phospy.apk"));
    }


    @Test
    public void testProxy_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "proxy_samp.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("proxy_samp.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("proxy_samp.apk"));
    }


    @Test
    public void testRemote_control_smack() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "remote_control_smack.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("remote_control_smack.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("remote_control_smack.apk"));
    }


    @Test
    public void testRepane() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "repane.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("repane.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("repane.apk"));
    }


    @Test
    public void testRoidsec() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "roidsec.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("roidsec.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("roidsec.apk"));
    }


    @Test
    public void testSamsapo() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "samsapo.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("samsapo.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("samsapo.apk"));
    }


    @Test
    public void testSave_me() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "save_me.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("save_me.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("save_me.apk"));
    }


    @Test
    public void testScipiex() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "scipiex.apk");
        app.setTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler.MethodFilter(
                Collections.singleton("SMSListenerService$1")
        )));
        app.addResultsAvailableHandler((cfg, results) -> compareResults("scipiex.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("scipiex.apk"));
    }


    @Test
    public void testSlocker_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "slocker_android_samp.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("slocker_android_samp.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("slocker_android_samp.apk"));
    }


    @Test
    public void testSms_google() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "sms_google.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("sms_google.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("sms_google.apk"));
    }


    @Test
    public void testSms_send_locker_qqmagic() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "sms_send_locker_qqmagic.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("sms_send_locker_qqmagic.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("sms_send_locker_qqmagic.apk"));
    }


    @Test
    public void testSmssend_packageinstaller() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "smssend_packageInstaller.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("smssend_packageInstaller.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("smssend_packageInstaller.apk"));
    }


    @Test
    public void testSmssilience_fake_vertu() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "smssilience_fake_vertu.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("smssilience_fake_vertu.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("smssilience_fake_vertu.apk"));
        Assert.assertEquals(0, results.getResultSet().size());
    }


    @Test
    public void testSmsstealer_kysn_assassincreed_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "smsstealer_kysn_assassincreed_android_samp.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("smsstealer_kysn_assassincreed_android_samp.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("smsstealer_kysn_assassincreed_android_samp.apk"));
    }


    @Test
    public void testStels_flashplayer_android_update() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "stels_flashplayer_android_update.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("stels_flashplayer_android_update.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("stels_flashplayer_android_update.apk"));
    }


    @Test
    public void testTetus() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "tetus.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("tetus.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("tetus.apk"));
        Assert.assertEquals(0, results.getResultSet().size());
    }

    @Test
    public void testThe_interview_movieshow() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "the_interview_movieshow.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("the_interview_movieshow.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("the_interview_movieshow.apk"));
        Assert.assertEquals(0, results.getResultSet().size());
    }


    @Test
    public void testThreatjapan_uracto() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "threatjapan_uracto.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("threatjapan_uracto.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("threatjapan_uracto.apk"));
        Assert.assertEquals(0, results.getResultSet().size());
    }


    @Test
    public void testVibleaker_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "vibleaker_android_samp.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("vibleaker_android_samp.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("vibleaker_android_samp.apk"));
    }


    @Ignore("CG Problem")
    @Test
    public void testXbot_android_samp() throws XmlPullParserException, IOException {
        SetupApplication app = initApplication(pathToAPKs + "/" + "xbot_android_samp.apk");
        app.addResultsAvailableHandler((cfg, results) -> compareResults("xbot_android_samp.apk", cfg, results));
        InfoflowResults results = app.runInfoflow(getSourcesAndSinks("xbot_android_samp.apk"));
    }
}
