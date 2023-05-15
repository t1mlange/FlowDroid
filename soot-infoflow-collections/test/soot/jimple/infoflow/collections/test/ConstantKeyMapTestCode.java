package soot.jimple.infoflow.collections.test;

import java.util.HashMap;
import java.util.Map;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class ConstantKeyMapTestCode {
    public void testMapPutGet1() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", tainted);
        String res = map.get("ConstantKey");
        sink(res);
    }

    public void testMapPutGet2() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", tainted);
        String res = map.get("OtherConstantKey");
        sink(res);
    }

    public void testMapPutGetOrDefault1() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", tainted);
        String res = map.getOrDefault("ConstantKey", "Untainted");
        sink(res);
    }

    public void testMapPutGetOrDefault2() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", tainted);
        String res = map.getOrDefault("OtherConstantKey", "Untainted");
        sink(res);
    }

    public void testMapPutGetOrDefault3() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", "Not tainted");
        String res = map.getOrDefault("NonConstantKey", tainted);
        sink(res);
    }

    public void testMapPutRemoveGet1() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", tainted);
        map.remove("ConstantKey");
        String res = map.get("ConstantKey");
        sink(res);
    }

    public void testMapClear1() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", tainted);
        map.clear();
        String res = map.get("ConstantKey");
        sink(res);
    }

    public void testMapKeySet1() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", tainted);
        String res = "";
        for (String e : map.keySet()) {
            if (e.equals("Some Value"))
                res = map.get(e);
        }
        sink(res);
    }

    public void testMapKeySet2() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", tainted);
        for (String e : map.keySet()) {
            if (e.equals("Some Value"))
                sink(e);
        }
    }

    public void testMapValueSet1() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", tainted);
        for (String e : map.values()) {
            if (e.equals("Some Value"))
                sink(e);
        }
    }

    public void testMapPutAll1() {
        Map<String, String> map = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();

        String tainted = source();
        map.put("ConstantKey", tainted);
        map2.putAll(map);
        sink(map2.get("ConstantKey"));
    }

    public void testMapPutAll2() {
        Map<String, String> map = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();

        String tainted = source();
        map.put("ConstantKey", tainted);
        map2.putAll(map);
        sink(map2.get("OtherKey"));
    }

    public void testMapPutIfAbsent1() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.putIfAbsent("ConstantKey", tainted);
        sink(map.get("ConstantKey"));
    }

    public void testMapPutIfAbsent2() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        String out = map.putIfAbsent("ConstantKey", tainted);
        sink(out);
    }

    public void testMapPutIfAbsent3() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.putIfAbsent("ConstantKey", tainted);
        String out = map.putIfAbsent("ConstantKey", "untainted");
        sink(out);
    }

    public void testMapPutIfAbsent4() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.putIfAbsent("ConstantKey", tainted);
        sink(map.get("OtherConstantKey"));
    }


    public void testMapPutIfAbsent5() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", "untainted");
        map.putIfAbsent("ConstantKey", tainted);
        sink(map.get("ConstantKey"));
    }
}
