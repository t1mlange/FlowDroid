package soot.jimple.infoflow.test.methodSummary;


/**
 * Test targets for making sure that we don't loose context sensitivity in our
 * approach, especially during path reconstruction
 * 
 * @author Steven Arzt
 *
 */
public class ContextSensitivity {

	private String x = null, y = null, z = null;
	
	void contextSensitivity1(Data d, String str1, String str2) {
		d.stringField = id(str1);
		d.stringField2 = id(str2);
	}
	
	String recursionTest1(String str) {
		System.out.println("run");
		z = y;
		y = x;
		x = str;
		if (z == null)
			return recursionTest1(y);
		return z;
	}
	
	private String id(String s) {
		return s;
	}

	String recursionTest2(String str) {
		y = x;
		if (y == null)
			return recursionTest2(y);
		return y;
	}

	class A {
		String str;
	}

	String recursionTest3(A str1, A str2) {
		if (str1 == null)
			return recursionTest3(str2, str1);
		str1.str = str2.str;
		return str1.str;
	}
}
