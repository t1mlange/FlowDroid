package soot.jimple.infoflow.test;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

public class StrongUpdateTestCode {
    class IntegerRef {
        int value;
    }

    public void strongUpdateTest1() {
        ConnectionManager cm = new ConnectionManager();
        IntegerRef i1 = new IntegerRef();
        IntegerRef i2 = i1;
        i2.value = TelephonyManager.getIMEI();
        i1.value = 42;
        cm.publish(i2.value);
    }

    public void strongUpdateTest2() {
        ConnectionManager cm = new ConnectionManager();
        IntegerRef i1 = new IntegerRef();
        IntegerRef i2 = i1;
        i2.value = TelephonyManager.getIMEI();
        i1.value = 42;
        i1.value = TelephonyManager.getIMEI();
        cm.publish(i2.value);
    }

    public void strongUpdateTest3() {
        ConnectionManager cm = new ConnectionManager();
        IntegerRef i1 = new IntegerRef();
        IntegerRef i2 = i1;
        i2.value = TelephonyManager.getIMEI();
        i1.value = addOne(41);
        cm.publish(i2.value);
    }

    public void strongUpdateTest4() {
        ConnectionManager cm = new ConnectionManager();
        IntegerRef i1 = new IntegerRef();
        IntegerRef i2 = i1;
        i2.value = TelephonyManager.getIMEI();
        i1.value = addOne(41);
        i1.value = TelephonyManager.getIMEI();
        cm.publish(i2.value);
    }

    private int addOne(int x) {
        return x+1;
    }
}
