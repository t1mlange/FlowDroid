package soot.jimple.infoflow.collections.test;

import java.util.Stack;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class ConstantIndexStackTestCode {
    public void testStackPushPop1() {
        Stack<String> s = new Stack<>();
        String tainted = source();
        s.push("First");
        s.push(tainted);
        s.push("Third");
        s.pop();
        String res = s.pop();
        sink(res);
    }

    public void testStackPushPop2() {
        Stack<String> s = new Stack<>();
        String tainted = source();
        s.push("First");
        s.push(tainted);
        s.push("Third");
        s.pop();
        s.pop();
        String res = s.pop();
        sink(res);
    }

    public void testStackPushPopPeek1() {
        Stack<String> s = new Stack<>();
        String tainted = source();
        s.push("First");
        s.push(tainted);
        s.push("Third");
        s.pop();
        String res = s.peek();
        sink(res);
    }
}
