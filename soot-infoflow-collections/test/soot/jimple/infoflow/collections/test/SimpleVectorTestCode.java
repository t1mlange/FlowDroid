package soot.jimple.infoflow.collections.test;

import java.util.Vector;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class SimpleVectorTestCode {

    public void testVectorFirstElement1() {
        Vector<String> v = new Vector<>();
        v.addElement(source());
        v.addElement("Test");
        sink(v.firstElement());
    }

    public void testVectorFirstElement2() {
        Vector<String> v = new Vector<>();
        v.addElement("Test");
        v.addElement(source());
        sink(v.firstElement());
    }

    public void testVectorHierarchySummaries1() {
        Vector<String> v = new Vector<>();
        v.add("Test");
        v.add(source());
        sink(v.get(1));
    }

    public void testVectorInsertElementAt1() {
        Vector<String> v = new Vector<>();
        v.addElement("Test");
        v.insertElementAt(source(), 0);
        sink(v.firstElement());
    }

    public void testVectorInsertElementAt2() {
        Vector<String> v = new Vector<>();
        v.addElement("Test");
        v.addElement("Test");
        v.insertElementAt(source(), 1);
        sink(v.firstElement());
    }

    public void testVectorRemoveAllElements1() {
        Vector<String> v = new Vector<>();
        v.addElement(source());
        v.addElement("Test");
        v.removeAllElements();
        sink(v.firstElement());
    }

    public void testVectorRemoveElementAt1() {
        Vector<String> v = new Vector<>();
        v.addElement(source());
        v.addElement("Test");
        v.removeElementAt(0);
        sink(v.firstElement());
    }

    public void testVectorRemoveElementAt2() {
        Vector<String> v = new Vector<>();
        v.addElement("Test");
        v.addElement(source());
        v.removeElementAt(0);
        sink(v.firstElement());
    }

    public void testVectorSetElementAt1() {
        Vector<String> v = new Vector<>();
        v.addElement("Test");
        v.addElement(source());
        v.setElementAt("XXX", 1);
        sink(v.get(1));
    }

    public void testVectorSetElementAt2() {
        Vector<String> v = new Vector<>();
        v.addElement("Test");
        v.addElement("XXX");
        v.setElementAt(source(), 1);
        sink(v.get(1));
    }
}
