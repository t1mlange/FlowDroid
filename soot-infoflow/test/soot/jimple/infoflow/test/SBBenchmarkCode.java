package soot.jimple.infoflow.test;

import org.json.JSONObject;
import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

import java.io.StringWriter;

public class SBBenchmarkCode {
    public void appendTest() {
        String id = TelephonyManager.getDeviceId();
        int imei = TelephonyManager.getIMEI();
        int imsi = TelephonyManager.getIMSI();
        StringBuilder sb = new StringBuilder();
        sb = sb.append("My device id is ").append(id).append(",my IMEI is ").append(imei).append(" and my IMSI is ").append(imsi);
        ConnectionManager cm = new ConnectionManager();
        cm.publish(sb.toString());
    }

    class SerializedObject {
        private int i = 42;
        private String str;
        private double d = 1.337;

        SerializedObject(String str) {
            this.str = str;
        }
    }

    public void JSONTest() {
        String tainted = TelephonyManager.getDeviceId();
        SerializedObject obj = new SerializedObject(tainted);
        JSONObject jsonO = new JSONObject(obj, new String[] { "i", "str", "d" }.toString());
        ConnectionManager cm = new ConnectionManager();
        cm.publish(jsonO.toString());
    }
}
