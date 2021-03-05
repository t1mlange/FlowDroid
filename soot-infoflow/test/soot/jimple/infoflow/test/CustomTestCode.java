package soot.jimple.infoflow.test;

import org.junit.Test;
import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.Location;
import soot.jimple.infoflow.test.android.LocationManager;
import soot.jimple.infoflow.test.android.TelephonyManager;
import soot.jimple.infoflow.test.utilclasses.ClassWithStatic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Simple test targets for very basic functions
 * 
 * @author Steven Arzt
 *
 */
public class CustomTestCode {
	private LList next(LList lst) {
		return lst.next;
	}

	public void retFieldRef() {
		LList l1 = new LList();
		l1.next = new LList();
		LList l2 = next(l1);
	}

	class LList {
		LList next;
		int data;
	}
	LList lst;
	public void testLList(){
		LList l1 = new LList();
		l1.next = new LList();
		l1.next.next = new LList();
		l1.next.next.data = TelephonyManager.getIMEI();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(l1.data);
		cm.publish(l1.next.data);
		cm.publish(l1.next.next.data);
	}
	public void testLListStatic(){
		lst = new LList();
		lst.next = new LList();
		lst.next.next = new LList();
		lst.next.next.data = TelephonyManager.getIMEI();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(lst.data);
		cm.publish(lst.next.data);
		cm.publish(lst.next.next.data);
	}

	static String staticStr;
	public void clinitLeak() {
		CustomTestCode.staticStr = TelephonyManager.getDeviceId();
		ClinitLeak c = new ClinitLeak();
	}



	public void bookkeepingTest() {
		IntegerRef i = new IntegerRef();
		method(i);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(i.value);
	}
	void method(IntegerRef i) {
		if (i.value == 1) {
			i.value = 2;
			i = source();
		}
//		else
//			i.value = TelephonyManager.getIMEI();
	}

	IntegerRef source() {
		return new IntegerRef(42);
	}
}

class IntegerRef {
	int value;

	IntegerRef(int value) {
		this.value = value;
	}
	IntegerRef() {
		this(0);
	}
}

class ClinitLeak {
	static {
		ConnectionManager staticCm = new ConnectionManager();
		staticCm.publish(CustomTestCode.staticStr);
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