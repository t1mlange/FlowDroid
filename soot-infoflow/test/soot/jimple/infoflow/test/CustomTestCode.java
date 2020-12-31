package soot.jimple.infoflow.test;

import org.junit.Test;
import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.Location;
import soot.jimple.infoflow.test.android.LocationManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

import java.util.ArrayList;

/**
 * Simple test targets for very basic functions
 * 
 * @author Steven Arzt
 *
 */
public class CustomTestCode {
	class IntegerRef {
		int value;
	}

	public void easyAliasTest() {
		IntegerRef i = new IntegerRef();
		IntegerRef j = i;
		j.value = TelephonyManager.getIMEI();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(i.value);
	}

	private void source(IntegerRef sourcei) {
		sourcei.value = TelephonyManager.getIMEI();
	}
	private void leak(IntegerRef leaki) {
		ConnectionManager cm = new ConnectionManager();
		cm.publish(leaki.value);
	}
	public void callAliasTest() {
		IntegerRef i = new IntegerRef();
		IntegerRef j = i;
		source(j);
		leak(i);
	}
	public void negativeCallAliasTest() {
		IntegerRef i = new IntegerRef();
		IntegerRef j = i;
		leak(i);
		source(j);
	}

	static String tainted = TelephonyManager.getDeviceId();
	public void clinitTest() {
		ConnectionManager cm = new ConnectionManager();
		cm.publish(tainted);
	}

	public void strongClinitTest() {
		TestClinit tc = new TestClinit();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(tc.get());
	}

	public void easyListTest() {
		ArrayList<String> lst = new ArrayList<>();
		lst.add(TelephonyManager.getDeviceId());

		ConnectionManager cm = new ConnectionManager();
		cm.publish(lst.get(0));
	}
}

class TestClinit {
	static String test = source();

	public String get() {
		return test;
	}

	private static String source() {
		return TelephonyManager.getDeviceId();
	}
}