package soot.jimple.infoflow.collections.test;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

import java.util.HashMap;
import java.util.Map;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

public class AbstractingTestCode {
    // This method doesn't alter the context and thus, doesn't depend on it
    String unusedContext1(Map<String, String> c) {
        StringBuilder sb = new StringBuilder();
        sb.append("Collection: ");
        sb.append(c.toString());
        return sb.toString();
    }

    String getXXX(Map<String, String> map) {
        return map.get("XXX");
    }

    String removeXXX(Map<String, String> map) {
        return map.get("XXX");
    }

    @FlowDroidTest(expected = 2)
    public void testReuse1() {
        Map<String, String> map = new HashMap<>();
        map.put("XXX", source());
        unusedContext1(map);
        sink(map.get("XXX"));

        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", source());
        unusedContext1(map2);
        sink(map2.get("YYY"));
    }

    @FlowDroidTest(expected = 2)
    public void testReuse2() {
        Map<String, String> map = new HashMap<>();
        map.put("XXX", source());
        sink(unusedContext1(map));

        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", source());
        sink(unusedContext1(map2));
    }

    @FlowDroidTest(expected = 2)
    public void testReuse3() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("XXX", tainted);
        unusedContext1(map);
        sink(map.get("XXX"));

        System.out.println("Delay");

        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", tainted);
        unusedContext1(map2);
        sink(map2.get("YYY"));
    }

    @FlowDroidTest(expected = 2)
    public void testReuse4() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("XXX", tainted);
        sink(unusedContext1(map));

        System.out.println("Delay");

        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", tainted);
        sink(unusedContext1(map2));
    }

    @FlowDroidTest(expected = 1)
    public void testGet1() {
        Map<String, String> map = new HashMap<>();
        map.put("XXX", source());
        sink(getXXX(map));


        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", source());
        sink(getXXX(map2));
    }

    @FlowDroidTest(expected = 1)
    public void testGet2() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("XXX", tainted);
        sink(getXXX(map));

        System.out.println("Delay");

        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", tainted);
        sink(getXXX(map2));
    }

    @FlowDroidTest(expected = 1)
    public void testRemove1() {
        Map<String, String> map = new HashMap<>();
        map.put("XXX", source());
        sink(removeXXX(map));


        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", source());
        sink(removeXXX(map2));
    }

    @FlowDroidTest(expected = 1)
    public void testRemove2() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("XXX", tainted);
        sink(removeXXX(map));

        System.out.println("Delay");

        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", tainted);
        sink(removeXXX(map2));
    }
}
