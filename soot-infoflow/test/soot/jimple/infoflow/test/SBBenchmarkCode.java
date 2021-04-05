package soot.jimple.infoflow.test;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

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
}
