package soot.jimple.infoflow.test;

import org.junit.Test;
import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.Location;
import soot.jimple.infoflow.test.android.LocationManager;
import soot.jimple.infoflow.test.android.TelephonyManager;
import soot.jimple.infoflow.test.utilclasses.ClassWithStatic;

import java.util.ArrayList;

/**
 * Simple test targets for very basic functions
 * 
 * @author Steven Arzt
 *
 */
public class CustomTestCode {
	class X {
//		private Object o;
		private Object[] arr;
	}
	private void doAlias(X b, X c) {
		b.arr = c.arr;
	}
	public void aliasTypeTest() {
		X b = new X();
		b.arr = new Object[2];
		X c = new X();

		doAlias(b, c);
		b.arr[0] = TelephonyManager.getDeviceId();

		X d = new X();
		doAlias(c, d);
		b.arr[1] = new String[] { TelephonyManager.getDeviceId() };

		ConnectionManager cm = new ConnectionManager();
		cm.publish((String) d.arr[0]);
	}

	public void testForEarlyTermination(){
		ConnectionManager cm = new ConnectionManager();
		cm.publish(ClassWithStatic.staticString);

		@SuppressWarnings("unused")
		ClassWithStatic c1 = new ClassWithStatic();

		WrapperClass w1 = new WrapperClass();
		w1.callIt(); // ClassWithStatic.staticString = source()
	}

	class WrapperClass{

		public void callIt(){
			ClassWithStatic.staticString = TelephonyManager.getDeviceId();
		}

		public void sink(){
			ConnectionManager cm = new ConnectionManager();
			cm.publish(ClassWithStatic.staticString);
		}

	}

//	class IntegerRef {
//		int value;
//	}
//
//	public void easyAliasTest() {
//		IntegerRef i = new IntegerRef();
//		IntegerRef j = i;
//		j.value = TelephonyManager.getIMEI();
//
//		ConnectionManager cm = new ConnectionManager();
//		cm.publish(i.value);
//	}
//
//	private void source(IntegerRef sourcei) {
//		sourcei.value = TelephonyManager.getIMEI();
//	}
//	private void leak(IntegerRef leaki) {
//		ConnectionManager cm = new ConnectionManager();
//		cm.publish(leaki.value);
//	}
//	public void callAliasTest() {
//		IntegerRef i = new IntegerRef();
//		IntegerRef j = i;
//		source(j);
//		leak(i);
//	}
//	public void negativeCallAliasTest() {
//		IntegerRef i = new IntegerRef();
//		IntegerRef j = i;
//		leak(i);
//		source(j);
//	}
//
//	static String tainted = TelephonyManager.getDeviceId();
//	public void clinitTest() {
//		ConnectionManager cm = new ConnectionManager();
//		cm.publish(tainted);
//	}
//
//	public void strongClinitTest() {
//		TestClinit tc = new TestClinit();
//		ConnectionManager cm = new ConnectionManager();
//		cm.publish(tc.get());
//	}
//
//	public void easyListTest() {
//		ArrayList<String> lst = new ArrayList<>();
//		lst.add(TelephonyManager.getDeviceId());
//
//		ConnectionManager cm = new ConnectionManager();
//		cm.publish(lst.get(0));
//	}
}

//class TestClinit {
//	static String test = source();
//
//	public String get() {
//		return test;
//	}
//
//	private static String source() {
//		return TelephonyManager.getDeviceId();
//	}
//}