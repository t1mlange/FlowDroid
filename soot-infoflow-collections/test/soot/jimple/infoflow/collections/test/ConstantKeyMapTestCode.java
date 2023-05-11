package soot.jimple.infoflow.collections.test;

import java.util.HashMap;
import java.util.Map;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class ConstantKeyMapTestCode {
    public void testMap1() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", tainted);
        String res = map.get("ConstantKey");
        sink(res);
    }

    public void testMap2() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", tainted);
        String res = map.get("OtherConstantKey");
        sink(res);
    }

    public void testMap3() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", tainted);
        map.remove("ConstantKey");
        String res = map.get("ConstantKey");
        sink(res);
    }

    public void testMap4() {
        Map<String, String> map = new HashMap<>();
        String tainted = source();
        map.put("ConstantKey", tainted);
        map.clear();
        String res = map.get("ConstantKey");
        sink(res);
    }

    public void testMap5() {
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

    public void testMap6() {
        Map<String, String> map = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();

        String tainted = source();
        map.put("ConstantKey", tainted);
        map2.putAll(map);
        sink(map2.get("ConstantKey"));
    }

    public void testMap7() {
        Map<String, String> map = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();

        String tainted = source();
        map.put("ConstantKey", tainted);
        map2.putAll(map);
        sink(map2.get("OtherKey"));
    }
}
