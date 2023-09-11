package soot.jimple.infoflow.collections.test;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

public class AliasListTestCode {
    List<String> alias;

    static class Data {
        String stringField;
    }

    @FlowDroidTest(expected = 1)
    public void testListAdd1() {
        alias = null;

        Data d = new Data();
        List<Data> lst = new ArrayList<>();
        lst.add(d);
        Data alias = lst.get(0);
        alias.stringField = source();
        sink(d.stringField);
    }

    @FlowDroidTest(expected = 1)
    public void testShiftOnAlias1() {
        alias = null;

        List<String> lst = new ArrayList<>();
        lst.add(source()); // lst@0 tainted
        alias = lst;
        alias.add(0, "element"); // lst@0 must shift to the right
        sink(lst.get(0));
        sink(lst.get(1)); // only right, should be possible to use the uniqueness property of the alloc site
        sink(lst.get(2));
    }

    @FlowDroidTest(expected = 1)
    public void testShiftOnAlias2() {
        alias = null;

        List<String> lst = new ArrayList<>();
        lst.add(source()); // lst@0 tainted
        alias = lst;
        alias.add(0, "element"); // lst@0 must shift to the right
        sink(alias.get(0));
        sink(alias.get(1)); // only right, should be possible to use the uniqueness property of the alloc site
        sink(alias.get(2));
    }

    static class ListWrapper {
        List<String> lst;
    }
    private ListWrapper alias2;

    @FlowDroidTest(expected = 1)
    public void testShiftOnAlias3() {
        alias2 = null;

        alias2 = new ListWrapper();
        List<String> lst = new ArrayList<>();
        lst.add(source()); // lst@0 tainted
        alias2.lst = lst;
        alias2.lst.add(0, "element"); // lst@0 must shift to the right
        sink(alias2.lst.get(0));
        sink(alias2.lst.get(1)); // only right, should be possible to use the uniqueness property of the alloc site
        sink(alias2.lst.get(2));
    }

    @FlowDroidTest(expected = 1)
    public void testListShiftAlias1() {
        alias = null;

        List<String> lst = new ArrayList<>();
        alias = lst;

        // We do not know the index, yet we should not shift
        // in the alias flow because our list size analysis
        // gave us the right index wrt to aliasing etc at the
        // alias query point. We only want to find aliases to
        // the list.
        lst.add(new Random().nextInt(), "unrelated");

        lst.add(source());

        // Check that only index 1 is correctly tainted
        sink(lst.get(0));
        sink(lst.get(1));
        sink(lst.get(2));
    }
}
