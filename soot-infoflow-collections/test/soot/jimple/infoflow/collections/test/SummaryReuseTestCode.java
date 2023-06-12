package soot.jimple.infoflow.collections.test;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class SummaryReuseTestCode {
    private String getElement(List<String> lst) {
        return lst.get(1);
    }

    // Ground truth would be 1, but we reuse the coarser summary for speed-up
    @FlowDroidTest(expected = 2)
    public void testListCoarserSummaryReuse() {
        String tainted = source();
        List<String> lst = new LinkedList<>();
        lst.add(tainted); // after: [0,0]
        lst.add(new Random().nextInt(), "Some Element"); // after: [0,1]
        sink(getElement(lst));

        // Delay the taint
        System.out.println(tainted);

        List<String> lst2 = new LinkedList<>();
        lst2.add(tainted); // after: [0,0]
        sink(getElement(lst2));
    }

    String id(String str) {
        return str;
    }

    @FlowDroidTest(expected = 2)
    public void testSummary() {
        String tainted = source();
        sink(id(tainted));
        sink(id(tainted));
    }
}
