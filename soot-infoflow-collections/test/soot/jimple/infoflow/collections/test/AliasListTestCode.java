package soot.jimple.infoflow.collections.test;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

import java.util.ArrayList;
import java.util.List;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class AliasListTestCode {
    List<String> alias;

    static class Data {
        String stringField;
    }

    @FlowDroidTest(expected = 1)
    public void testListAdd1() {
        Data d = new Data();
        List<Data> lst = new ArrayList<>();
        lst.add(d);
        Data alias = lst.get(0);
        alias.stringField = source();
        sink(d.stringField);
    }
}
