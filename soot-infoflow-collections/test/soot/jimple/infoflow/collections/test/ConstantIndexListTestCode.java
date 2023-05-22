package soot.jimple.infoflow.collections.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    public void testListRemove3() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.remove(0);
        sink(lst.get(0));
    }

    public void testListRemove4() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.add("Some String");
        lst.remove(0);
        sink(lst.get(1));
    }

    public void testListRemove5() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.add("Some String");
        lst.remove("String");
        sink(lst.get(0));
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

    public void testListInsert4() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.add(new Random().nextInt(), "Other");
        lst.add("xxx");
        sink(lst.get(1)); // index 1 or 2 might be tainted
    }

    public void testListInsert5() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.add(new Random().nextInt(), "Other");
        lst.add("xxx");
        sink(lst.get(2)); // index 1 or 2 might be tainted
    }

    public void testListInsert6() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.add(new Random().nextInt(), "Other");
        lst.add("xxx");
        sink(lst.get(3)); // index 1 or 2 might be tainted
    }

    public void testListInsertInLoop1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add(source()); // tainted @ idx=0
        while (new Random().nextBoolean()) {
            lst.add(0, "Some element"); // tainted idx gets shifted by 1 each iteration
        }
        sink(lst.get(0));
    }

    public void testListReplaceAll1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add(tainted);
        lst.replaceAll(e -> e);
        sink(lst.get(0));
    }

    public void testListReplaceAll2() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add(tainted);
        lst.replaceAll(e -> "Overwritten");
        sink(lst.get(0));
    }

    public void testListSublist1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add(tainted);
        lst.add("xxx");
        lst.add("yyy");
        List<String> subList = lst.subList(0, 1);
        sink(subList.get(1));
    }

    public void testListSublist2() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add(tainted);
        lst.add("xxx");
        lst.add("yyy");
        List<String> subList = lst.subList(0, 1);
        sink(lst.get(1));
    }
}
