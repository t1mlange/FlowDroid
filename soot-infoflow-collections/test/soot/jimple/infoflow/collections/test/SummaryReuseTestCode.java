package soot.jimple.infoflow.collections.test;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class SummaryReuseTestCode {
    private String getElementOne(List<String> lst) {
        return lst.get(1);
    }

    // Ground truth would be 1, but we reuse the coarser summary for speed-up
    @FlowDroidTest(expected = 2)
    public void testListCoarserSummaryReuse1() {
        String tainted = source();
        List<String> lst = new LinkedList<>();
        lst.add(tainted); // after: [0,0]
        lst.add(new Random().nextInt(), "Some Element"); // after: [0,1]
        sink(getElementOne(lst));

        System.out.println("Delay");

        List<String> lst2 = new LinkedList<>();
        lst2.add(tainted); // after: [0,0]
        sink(getElementOne(lst2));
    }

    private String getElementTwo(List<String> lst) {
        return lst.get(2);
    }

    // Tests that only more coarse summaries are used
    @FlowDroidTest(expected = 1)
    public void testListCoarserSummaryReuse2() {
        String tainted = source();
        List<String> lst = new LinkedList<>();
        lst.add(tainted); // after: [0,0]
        lst.add(new Random().nextInt(), "Some Element"); // after: [0,1]
        // getElementTwo now has a summary for lst[0,1]
        sink(getElementTwo(lst));

        List<String> lst2 = new LinkedList<>();
        lst2.add(tainted); // after: [0,0]
        lst2.add(new Random().nextInt(), "Some Element"); // after: [0,1]
        lst2.add(new Random().nextInt(), "Some Element"); // after: [0,2]
        // If it had used the previous summary, we wouldn't have a leak here
        sink(getElementTwo(lst2));
    }

    // Tests that the most precise available summary is used
    @FlowDroidTest(expected = 1)
    public void testListCoarserSummaryReuse3() {
        String tainted = source();
        List<String> lst = new LinkedList<>();
        lst.add(tainted); // after: [0,0]
        lst.add(new Random().nextInt(), "Some Element"); // after: [0,1]
        // getElementTwo now has a summary for lst[0,1]
        sink(getElementTwo(lst));

        List<String> lst2 = new LinkedList<>();
        lst2.add(tainted); // after: [0,0]
        lst2.add(new Random().nextInt(), "Some Element"); // after: [0,1]
        lst2.add(new Random().nextInt(), "Some Element"); // after: [0,2]
        // getElementTwo now has a summary for lst[0,2]
        sink(getElementTwo(lst2));

        List<String> lst3 = new LinkedList<>();
        lst3.add(tainted); // after: [0,0]
        // No leak unless the less precise lst[0,2] summary is used
        sink(getElementTwo(lst3));
    }

    String id(String str) {
        return str;
    }

    @FlowDroidTest(expected = 2)
    public void testNoTaintGetsLost() {
        String tainted;
        if (new Random().nextBoolean())
            tainted = source();
        else
            tainted = source();
        sink(id(tainted));
    }
}
