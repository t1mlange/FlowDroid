package soot.jimple.infoflow.collections.test;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class IteratorTestCode {
    @FlowDroidTest(expected = 2)
    public void testIterator1() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        // We can keep the index because the iterator is only used for reading
        for (String str : lst)
            sink(str); // Correct
        sink(lst.get(0)); // Correct
        sink(lst.get(1)); // Incorrect
    }

    @FlowDroidTest(expected = 2)
    public void testIterator2() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        // We can keep the index because the iterator is only used for reading
        ListIterator<String> it = lst.listIterator(1);
        sink(it.previous()); // Correct
        sink(lst.get(0)); // Correct
        sink(lst.get(1)); // Incorrect
    }

    private void calleeUsingIterator(Iterator<String> it) {
        //
    }

    @FlowDroidTest(expected = 3)
    public void testIterator3() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        Iterator<String> it = lst.iterator();
        // We have to discard the index because the callee
        // might do things with the iterator
        calleeUsingIterator(it);
        sink(it.next()); // Correct
        sink(lst.get(0)); // Correct
        sink(lst.get(1)); // Correct
    }

    public Iterator<String> getIterator(List<String> lst) {
        // We have to discard the index because the iterator leaves the method
        return lst.iterator();
    }

    @FlowDroidTest(expected = 3)
    public void testIterator4() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        Iterator<String> it = getIterator(lst);
        sink(it.next()); // Correct
        sink(lst.get(0)); // Correct
        sink(lst.get(1)); // Correct
    }

    private Iterator<String> f;
    @FlowDroidTest(expected = 3)
    public void testIterator5() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        // We have to discard the index because if flows into a field
        this.f = lst.listIterator();
        sink(f.next()); // Correct
        sink(lst.get(0)); // Correct
        sink(lst.get(1)); // Correct
    }

    @FlowDroidTest(expected = 2)
    public void testIterator6() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        // We can keep the iterator as it is only casted
        Iterator<String> it = lst.iterator();
        ListIterator<String> lit = (ListIterator<String>) it;
        sink(lit.next()); // Correct
        sink(lst.get(0)); // Correct
        sink(lst.get(1)); // Incorrect
    }

    @FlowDroidTest(expected = 3)
    public void testIterator7() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        // We must remove the index because there is a remove call on the alias
        Iterator<String> it = lst.iterator();
        ListIterator<String> lit = (ListIterator<String>) it;
        sink(lit.next()); // Correct
        lit.remove();
        sink(lst.get(0)); // Correct
        sink(lst.get(1)); // Correct
    }

    @FlowDroidTest(expected = 2)
    public void testIterator8() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        List<String> subLst = lst.subList(0, 0);
        List<String> subLst2 = subLst.subList(0, 0);
        Iterator<String> it = subLst2.iterator();
        ListIterator<String> lit = (ListIterator<String>) it;
        sink(lit.next()); // Correct
        sink(lst.get(0)); // Correct
        sink(lst.get(1)); // Incorrect
    }

    @FlowDroidTest(expected = 3)
    public void testIterator9() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        List<String> subLst = lst.subList(0, 0);
        List<String> subLst2 = subLst.subList(0, 0);
        Iterator<String> it = subLst2.iterator();
        ListIterator<String> lit = (ListIterator<String>) it;
        sink(lit.next()); // Correct
        lit.remove();
        sink(lst.get(0)); // Correct
        sink(lst.get(1)); // Correct
    }

    @FlowDroidTest(expected = 1)
    public void testIterator10() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        List<String> subLst = lst.subList(0, 0);
        List<String> subLst2 = subLst.subList(0, 0);
        System.out.println(subLst2.size());
        sink(lst.get(0)); // Correct
        sink(lst.get(1)); // Correct
    }

    @FlowDroidTest(expected = 2)
    public void testIterator11() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        List<String> subLst = lst.subList(0, 0);
        List<String> subLst2 = subLst.subList(0, 0);
        subLst2.remove(0);
        sink(lst.get(0)); // Correct
        sink(lst.get(1)); // Correct
    }
}
