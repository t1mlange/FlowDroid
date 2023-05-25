package soot.jimple.infoflow.collections.test;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.Map;
import java.util.Set;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class SimpleTableTestCode {
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
        t.put("Row", "Col", source());
        Map<String, String> map = t.row("Row");
        sink(map.get("Col"));
    }

    public void testTableRow2() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        Map<String, String> map = t.row("Col");
        sink(map.get("Row"));
        sink(map.get("Col"));
    }

    public void testTableRow3() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        Map<String, String> map = t.row("Row");
        sink(map.get("Row"));
    }

    public void testTableColumn1() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        Map<String, String> map = t.column("Col");
        sink(map.get("Row"));
    }

    public void testTableColumn2() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        Map<String, String> map = t.column("Row");
        sink(map.get("Row"));
        sink(map.get("Col"));
    }

    public void testTableColumn3() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        Map<String, String> map = t.column("Col");
        sink(map.get("Col"));
    }

    public void testTableCellSet1() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Key", "Key", source());
        Set<Table.Cell<String, String, String>> set = t.cellSet();
        for (Table.Cell<String, String, String> cell : set) {
            sink(cell.getValue());
        }
    }

    public void testTableCellSet2() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Key", "Key", source());
        Set<Table.Cell<String, String, String>> set = t.cellSet();
        sink(set.stream().findAny().get().getColumnKey());
    }

    public void testTableColumnMap1() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        Map<String, Map<String, String>> map = t.columnMap();
        sink(map.get("Col").get("Row"));
    }

    public void testTableColumnMap2() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        Map<String, Map<String, String>> map = t.columnMap();
        sink(map.get("Row").get("Col"));
    }

    public void testTableRowMap1() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        Map<String, Map<String, String>> map = t.rowMap();
        sink(map.get("Row").get("Col"));
    }

    public void testTableRowMap2() {
        Table<String, String, String> t = HashBasedTable.create();
        t.put("Row", "Col", source());
        Map<String, Map<String, String>> map = t.rowMap();
        sink(map.get("Col").get("Row"));
    }
}
