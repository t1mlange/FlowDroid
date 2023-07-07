package soot.jimple.infoflow.collections.test;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

public class AbstractingTestCode {
    // This method doesn't alter the context and thus, doesn't depend on it
    String unusedContext1(Map<String, String> c) {
        StringBuilder sb = new StringBuilder();
        sb.append("Collection: ");
        sb.append(c.toString());
        return sb.toString();
    }

    // but getXXX and removeXXX depend on the context
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

    private String badCallee1(Map<String, String> map) {
        return getXXX(map);
    }

    private String badCallee2(Map<String, String> map) {
        return removeXXX(map);
    }

    @FlowDroidTest(expected = 1)
    public void testReinjectInCallee1() {
        Map<String, String> map = new HashMap<>();
        map.put("XXX", source());
        sink(badCallee1(map));


        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", source());
        sink(badCallee1(map2));
    }

    @FlowDroidTest(expected = 1)
    public void testReinjectInCallee2() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("XXX", tainted);
        sink(badCallee1(map));

        System.out.println("Delay");

        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", tainted);
        sink(badCallee1(map2));
    }

    @FlowDroidTest(expected = 1)
    public void testReinjectInCallee3() {
        Map<String, String> map = new HashMap<>();
        map.put("XXX", source());
        sink(badCallee2(map));


        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", source());
        sink(badCallee2(map2));
    }

    @FlowDroidTest(expected = 1)
    public void testReinjectInCallee4() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("XXX", tainted);
        sink(badCallee2(map));

        System.out.println("Delay");

        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", tainted);
        sink(badCallee2(map2));
    }

    Map<String, String> f;
    private String calleeOfBadCallee1(Map<String, String> map) {
        f = map;
        Map<String, String> alias = f;
        return getXXX(alias);
    }

    private String calleeOfBadCallee2(Map<String, String> map) {
        f = map;
        Map<String, String> alias = f;
        return removeXXX(map);
    }

    @FlowDroidTest(expected = 1)
    public void testReinjectInCalleeOfCallee1() {
        Map<String, String> map = new HashMap<>();
        map.put("XXX", source());
        sink(calleeOfBadCallee1(map));


        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", source());
        sink(calleeOfBadCallee1(map2));
    }

    @FlowDroidTest(expected = 1)
    public void testReinjectInCalleeOfCallee2() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("XXX", tainted);
        sink(calleeOfBadCallee1(map));

        System.out.println("Delay");

        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", tainted);
        sink(calleeOfBadCallee1(map2));
    }

    @FlowDroidTest(expected = 1)
    public void testReinjectInCalleeOfCallee3() {
        Map<String, String> map = new HashMap<>();
        map.put("XXX", source());
        sink(calleeOfBadCallee2(map));


        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", source());
        sink(calleeOfBadCallee2(map2));
    }

    @FlowDroidTest(expected = 1)
    public void testReinjectInCalleeOfCallee4() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("XXX", tainted);
        sink(calleeOfBadCallee2(map));

        System.out.println("Delay");

        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", tainted);
        sink(calleeOfBadCallee2(map2));
    }

    @FlowDroidTest(expected = 2)
    public void testReinjectOnAlreadySeenCallee1() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("XXX", tainted);
        // Here we notice getXXX() is context-dependent
        sink(getXXX(map));

        Map<String, String> map2 = new HashMap<>();
        map2.put("XXX", tainted);
        Map<String, String> map3 = new HashMap<>();
        map3.put("YYY", tainted);
        // Here we start two calls with different keys such that the keys are
        // merged. Later on in the call tree, the solver should encounter that
        // getXXX is not reusable and reinject the abstraction correctly.
        String r1 = badCallee1(map2);
        String r2 = badCallee1(map3);
        sink(r1);
        sink(r2);
    }

    @FlowDroidTest(expected = 1)
    public void testReinjectOnAlreadySeenCallee2() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("XXX", tainted);
        sink(getXXX(map));

        map.remove("XXX");

        map.put("YYY", tainted);
        String r1 = badCallee1(map);
        sink(r1);
    }
}
