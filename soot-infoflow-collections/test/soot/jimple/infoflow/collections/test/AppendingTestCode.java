package soot.jimple.infoflow.collections.test;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

import java.util.*;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

public class AppendingTestCode {
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
        System.out.println("Delay to ensure appending");

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
        //       2.1. ...on the same collection type as the ref in the method. Then, the symbolic access path might cut
        //            the access path down if the key is equal.
        //       2.2. ...on a different collection type, where it would provide an advantage. But bear in mind that
        //            we are probably not able to deduce the list size correctly if the flow is too complex.
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

    private static class StringWrapper {
        String s;

        StringWrapper(String s) {
            this.s = s;
        }
    }

    private String unusedContextDerefAll(Map<String, StringWrapper> map) {
        StringBuilder sb = new StringBuilder();
        for (StringWrapper w : map.values())
            sb.append(w.s);
        return sb.toString();
    }

    @FlowDroidTest(expected = 2)
    public void testDerefOfAllFields() {
        Map<String, StringWrapper> map = new HashMap<>();
        StringWrapper src = new StringWrapper(source());
        map.put("Secret", src);
        String x = unusedContextDerefAll(map);
        sink(x);

        Map<String, StringWrapper> map2 = new HashMap<>();
        map2.put("Secret2", src);
        String x2 = unusedContextDerefAll(map2);
        sink(x2);
    }

    private static class MapContainer<T> {
        Map<T, T> map;

        MapContainer(Map<T, T> map) {
            this.map = map;
        }
    }

    private MapContainer<String> unusedContextAddRef(Map<String, String> map) {
        return new MapContainer<>(map);
    }

    @FlowDroidTest(expected = 2)
    public void testRefInCallee() {
        Map<String, String> map = new HashMap<>();
        String source = source();
        map.put("Secret", source);
        MapContainer<String> mc = unusedContextAddRef(map);
        sink(mc.map.get("Secret"));
        sink(mc.map.get("Secret2"));

        Map<String, String> map2 = new HashMap<>();
        map2.put("Secret2", source);
        MapContainer<String> mc2 = unusedContextAddRef(map2);
        sink(mc2.map.get("Secret"));
        sink(mc2.map.get("Secret2"));
    }

    private String benignRace(Map<String, String> map) {
        if (new Random().nextBoolean()) {
            // This will delay this path by some seconds while the else path is not delayed. This should result in
            // appending the similar abstraction and applying the identity flow as a summary before this path reached
            // the getXXX and subsequently marks the whole method as non-reusable. This is a race but won't change the
            // outcome because the else path is context-independent.
            System.out.println("Delay");
            return getXXX(map);
        } else {
            return null;
        }
    }

    @FlowDroidTest(expected = 2)
    public void testBenignRace1() {
        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        String source = source();
        map1.put("XXX", source);
        map2.put("YYY", source);
        String result1 = benignRace(map1);
        String result2 = benignRace(map2);
        sink(result1);
        sink(result2);
        sink(map1.get("XXX"));
        sink(map2.get("XXX"));
    }

    private Set<String> flatten(Map<String, Map<String, String>> map) {
        Set<String> set = new HashSet<>();
        for (Map<String, String> inner : map.values())
            for (String str : inner.values())
                set.add(str);
        return set;
    }

    @FlowDroidTest(expected = 4)
    public void testTwoContexts1() {
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> innerMap = new HashMap<>();
        innerMap.put("Inner", source());
        map.put("Outer", innerMap);
        Set<String> res1 = flatten(map);

        Map<String, Map<String, String>> map2 = new HashMap<>();
        Map<String, String> innerMap2 = new HashMap<>();
        innerMap2.put("Outer", source());
        map2.put("Inner", innerMap2);
        Set<String> res2 = flatten(map2);

        // The correct leak
        sink(map.get("Outer").get("Inner"));
        sink(map2.get("Inner").get("Outer"));
        sink(res1);
        sink(res2);
        // The false negatives
        sink(map.get("Outer").get("Outer"));
        sink(map.get("Inner").get("Outer"));
        sink(map.get("Inner").get("Inner"));
        sink(map2.get("Outer").get("Inner"));
        sink(map2.get("Outer").get("Outer"));
        sink(map2.get("Inner").get("Inner"));
    }

    @FlowDroidTest(expected = 4)
    public void testTwoContexts2() {
        Map<String, Map<String, String>> map = new HashMap<>();
        String source = source();
        Map<String, String> innerMap = new HashMap<>();
        innerMap.put("Inner", source);
        map.put("Outer", innerMap);
        Set<String> res1 = flatten(map);

        System.out.println("delay");

        Map<String, Map<String, String>> map2 = new HashMap<>();
        Map<String, String> innerMap2 = new HashMap<>();
        innerMap2.put("Inner", source);
        map2.put("Outer", innerMap2);
        Set<String> res2 = flatten(map2);

        // The correct leak
        sink(map.get("Outer").get("Inner"));
        sink(map2.get("Inner").get("Outer"));
        sink(res1);
        sink(res2);
        // The false negatives
        sink(map.get("Outer").get("Outer"));
        sink(map.get("Inner").get("Outer"));
        sink(map.get("Inner").get("Inner"));
        sink(map2.get("Outer").get("Inner"));
        sink(map2.get("Outer").get("Outer"));
        sink(map2.get("Inner").get("Inner"));
    }

    private String nestedCallee(Map<String, String> map) {
        // Neither can we reinject here because the taints depend on the return value of usesContext.
        // Another path might not have yet needed the context and if we remove it, it would miss a flow.
        // If we keep it, the return of usesContext would get wrong at the point it sees the similar abstraction
        // at a return edge.
        if (new Random().nextBoolean())
            return usesContext(map);
        else
            return "XXX";
    }

    private String usesContext(Map<String, String> map) {
        // Now assume we reinject the similar abstraction at map.get(), we'd remove the
        // abstractions from the similar abstractions and proceed to analyze the callee.
        // But there might be multiple paths, here the else case. Because we needed to remove
        // the abstractions from our data structure, the return in the else case doesn't know
        // the appended abstraction anymore and misses a flow. Keeping the similar abstraction
        // doesn't work either because obviously that would be wrong in the if case. Again,
        // path sensitivity is missing for this.
        if (new Random().nextBoolean())
            return map.get("Key");
        else
            for (String s : map.values())
                return s;
        return null;
    }

    @FlowDroidTest(expected = 1)
    public void testWhyReinjectAtUsageIsntPossible() {
        Map<String, String> map = new HashMap<>();
        map.put("XXX", source());
        sink(nestedCallee(map));
    }
}
