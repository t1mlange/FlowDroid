package soot.jimple.infoflow.collections.test;

import java.util.HashMap;
import java.util.Map;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class AliasMapTestCode {
    static class A {
        String s;
    }

    public Map<String, String> f;

    public void testMapPutGet1() {
        Map<String, String> m = new HashMap<>();
        f = m;
        m.put("Secret", source());
        sink(f.get("Secret"));
    }

    public void testMapPutGet2() {
        Map<String, String> m = new HashMap<>();
        m.put("Secret", source());
        sink(f.get("Secret"));
        f = m;
    }

    public void testMapPutGet3() {
        Map<String, A> m = new HashMap<>();
        A a = new A();
        m.put("Secret", a);
        a.s = source();
        A b = m.get("Secret");
        sink(b.s);
    }

    public void testMapPutGet4() {
        Map<String, A> m = new HashMap<>();
        A a = new A();
        m.put("Key", a);
        a.s = source();
        A b = m.get("Secret");
        sink(b.s);
    }

    public void testMapPutGet5() {
        Map<String, A> m = new HashMap<>();
        A a = new A();
        m.put("Key", new A());
        A b = m.put("Key", a);
        a.s = source();
        sink(b.s);
    }

    public void testMapPutGet6() {
        Map<String, A> m = new HashMap<>();
        A a = new A();
        m.put("Key", a);
        A b = m.put("Key", new A());
        a.s = source();
        sink(b.s);
    }

    public void testMapPutGet7() {
        Map<String, A> m = new HashMap<>();
        A a = new A();
        a.s = source();
        A b = m.get("Secret");
        sink(b.s);
        m.put("Secret", a);
    }

    public void testMapPutGet8() {
        Map<String, A> m = new HashMap<>();
        A a = new A();
        a.s = source();
        A b = m.get("Secret");
        sink(b.s);
        m.put("Secret", a);
    }
}
