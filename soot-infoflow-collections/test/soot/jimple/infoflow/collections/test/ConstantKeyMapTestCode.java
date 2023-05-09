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
}
