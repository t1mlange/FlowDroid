package soot.jimple.infoflow.collections.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class ConstantIndexListTestCode {
    public void testList1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        sink(lst.get(1));
    }

    public void testList2() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        sink(lst.get(0));
    }

    public void testList3(int x) {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        if (x == 1)
            lst.add(tainted);
        else
            lst.add("Not tainted");
        sink(lst.get(1));
    }

    public void testList4() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.remove(1);
        sink(lst.get(1));
    }

    public void testList5() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        String removed =lst.remove(1);
        sink(removed);
    }

    public void testList6() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.clear();
        sink(lst.get(1));
    }
}
