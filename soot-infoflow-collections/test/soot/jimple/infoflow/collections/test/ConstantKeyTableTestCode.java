package soot.jimple.infoflow.collections.test;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.Map;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class ConstantKeyTableTestCode {
    public void testTablePutGet1() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        sink(t.get("Row", "Col"));
    }

    public void testTablePutGet2() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col2", source());
        sink(t.get("Row", "Col"));
    }

    public void testTablePutGet3() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row2", "Col", source());
        sink(t.get("Row", "Col"));
    }

    public void testTablePutRemove1() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        t.remove("Row", "Col");
        sink(t.get("Row", "Col"));
    }

    public void testTablePutRemove2() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        String returned = t.remove("Row", "Col");
        sink(returned);
    }

    public void testTablePutRemove3() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        String returned = t.remove("Row", "Col2");
        sink(returned);
    }

    public void testTablePutAll1() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        Table<String, String, String> t2 = HashBasedTable.create();
        t2.putAll(t);
        sink(t2.get("Row", "Col"));
    }

    public void testTablePutAll2() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        Table<String, String, String> t2 = HashBasedTable.create();
        t2.putAll(t);
        sink(t2.get("Row", "Col2"));
    }

    public void testTableRow1() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Key", "Key", source());
        Map<String, String> map = t.row("Key");
        sink(map.get("Key"));
    }

    public void testTableRow2() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Key", "Key", source());
        Map<String, String> map = t.row("Key");
        sink(map.get("Key2"));
    }

    public void testTableRow3() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Key", "Key", source());
        Map<String, String> map = t.row("Key2");
        sink(map.get("Key2"));
    }
}
