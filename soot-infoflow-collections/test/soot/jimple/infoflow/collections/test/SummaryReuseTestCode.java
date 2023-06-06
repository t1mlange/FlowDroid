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

    // Ground truth would be 0, but we reuse the coarser summary for speed-up
    @FlowDroidTest(expected = 1)
    public void testListCoarserSummaryReuse() {
        List<String> lst = new LinkedList<>();
        lst.add(source()); // after: [0,0]
        lst.add(new Random().nextInt(), "Some Element"); // after: [0,1]
        sink(getElement(lst));

        List<String> lst2 = new LinkedList<>();
        lst2.add(source()); // after: [0,0]
        sink(getElement(lst2));
    }
}
