package soot.jimple.infoflow.collections.test;

import java.util.ArrayList;
import java.util.List;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class ConstantIndexListTestCode {
    public void testListAdd1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        sink(lst.get(1));
    }

    public void testListAdd2() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        sink(lst.get(0));
    }

    public void testListAdd3(int x) {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        if (x == 1)
            lst.add(tainted);
        else
            lst.add("Not tainted");
        sink(lst.get(1));
    }

    public void testListRemove1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.remove(1);
        sink(lst.get(1));
    }

    public void testListRemove2() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        String removed = lst.remove(1);
        sink(removed);
    }

    public void testListClear() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.clear();
        sink(lst.get(1));
    }

    public void testListInsert1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.add(1, "Other");
        lst.add("xxx");
        sink(lst.get(2));
    }

    public void testListInsert2() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.add(1, "Other");
        lst.add("xxx");
        sink(lst.get(1));
    }

    public void testListInsert3() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add("Some String2");
        lst.add("Some String3");
        lst.add(1, tainted);
        lst.add("xxx");
        sink(lst.get(1));
    }
}
