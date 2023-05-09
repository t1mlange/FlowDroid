package soot.jimple.infoflow.collections.test;

public class Helper {
    public static String source() {
        return "secret";
    }

    public static void sink(String str) {
        System.out.println(str);
    }
}
