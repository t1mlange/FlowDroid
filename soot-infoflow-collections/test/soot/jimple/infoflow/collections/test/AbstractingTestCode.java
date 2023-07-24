package soot.jimple.infoflow.collections.test;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

import java.util.*;

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

    private void calleeWithDerefRef(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        // Deref with a context-independent map operation
        for (String val : map.values())
            sb.append(val);
        // Put a value retrieved from the deref'd map, again
        // not dependent on the keys in the access path
        map.put("XXX", sb.toString());

        // There are two different equivalence classes: map.values@["XXX"] and map.values with any other key.
        // For map.values@["XXX"] only the identity is correct. While for any other key, "XXX" and the identity are a
        // correct summary for this method.
        // Now let's assume we see the key "XXX" first and append another key "YYY" to the propagation. At the return,
        // we just see the identity. So we would replace the context when mapping our appended abstraction back to the
        // caller. However, this would leak to a missed path. We cannot distinguish the case deref->ref from the
        // identity just based on the calling context and the current abstraction at a return site.
        //
        // There are two ways to resolve this:
        //     a) prohibit context-independent derefs for appending
        //     b) prohibit context-independent refs for appending
        // But only b) makes sense. Let me explain this by a counterexample to the usefulness of a) by looking
        // at two cases:
        //    1. The calling context has no context yet. Then, there won't be any appending because the default IFDS
        //       value approach will summarize as much as possible for the given access path.
        //    2. The calling context already contains a context...
        //       2.1. ...on the same collection type as the ref in the method. Then, the symbolic access path will cut
        //            the access path down due to recursion (map.values...values -> map.values).
        //       2.2. ...on a different collection type. This is the only way that appending actually provides an
        //            advantage.
        // On the other hand, a) can be applied all the time: for each context-independent operation, for each
        // invalidating operation.
    }

    @FlowDroidTest(expected = 2)
    public void testShowingWhyAddIsContextDependent() {
        Map<String, String> map = new HashMap<>();
        String source = source();
        map.put("XXX", source);
        calleeWithDerefRef(map);
        sink(map.get("XXX"));

        Map<String, String> map2 = new HashMap<>();
        map2.put("YYY", source);
        calleeWithDerefRef(map2);
        sink(map2.get("XXX"));
    }
}
