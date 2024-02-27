package soot.jimple.infoflow.test;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

import java.util.Random;

public class MergeReplayTestCode {
    static class Book {
        String name;

        void uselessop() {
            System.out.println("XXX");
        }
    }

    Book f;
    public void neighborProblem() {
        ConnectionManager cm = new ConnectionManager();
        Book b = new Book();
        f = b; // f aliases b
        if (new Random().nextBoolean()) {
            b.name = TelephonyManager.getDeviceId(); // _f|if gets activated
        } else {
            b.name = TelephonyManager.getDeviceId(); // _f|else gets activated
            String stack9 = f.name; // f.name -> stack9, f.name is kept as identity without clone
            cm.publish(stack9); // stack9 originating from b.name in else is leaked
//            System.out.println("DELAY");
        }
        // f.name from else reaches the neighbor merge with the same instance from which stack9
        // was derived. Activated f.name from if reaches this point as well and gets set as a neighbor
        // of f.name from else, the predecessor of stack9. Thus, the abstraction graph contains a path from
        // cm.publish to getDeviceId in the if case.
        System.out.println("neighbor merge point");
    }


    void id(Object o) {
        System.out.println("in id");
    }

    public void replaceAUWithGAU1() {
        ConnectionManager cm = new ConnectionManager();
        Book b = new Book();
        System.out.println("before id");
        id(b);
        b.name = TelephonyManager.getDeviceId();
        cm.publish(b.name);
    }

    public void callerOfReplaceAUWithGAU2() {
        ConnectionManager cm = new ConnectionManager();
        Book b = new Book();
        cm.publish(b.name);
        replaceAUWithGAU2(b);
        cm.publish(b.name);
    }

    public void replaceAUWithGAU2(Book b) {
        b.name = TelephonyManager.getDeviceId();
    }

    public void duplicatePropagation2() {
        ConnectionManager cm = new ConnectionManager();
        Book b = new Book();
        f = b;
        taintTwo(b);
        cm.publish(f.name);
    }

    void taintTwo(Book b) {
        if (new Random().nextBoolean()) {
            b.name = TelephonyManager.getDeviceId();
        } else {
            b.name = TelephonyManager.getDeviceId();
        }
    }

    Book pc;
    public void duplicatePropagation1() {
        Book qc = new Book();
        Book pc = qc;
        Book rc = new Book();
        setAndLeak(pc, qc, rc);
    }

    void setAndLeak(Book p, Book q, Book r) {
        ConnectionManager cm = new ConnectionManager();
        if (new Random().nextBoolean()) {
            p.name = TelephonyManager.getDeviceId();
        } else {
            p.name = TelephonyManager.getDeviceId();
            cm.publish(q.name);
        }
        cm.publish(r.name);
    }

    public void duplicatePropagationAndFP1() {
        Book q = new Book();
        Book p = q;
        Book r = new Book();
        setAndLeak(p, q, r);
        // I have fixed the turn around positions some time ago.
        // The original example from the MergeDroid paper works
        // on an older version where aliases were injected at
        // IdentityStmts. We now already detect at the call site
        // that two parameters aliased. Therefore, this statement
        // needs to be after the call to produce a false positive.
        intermediateCallee(q);
    }

    void intermediateCallee(Book q) {
        Book p = new Book();
        Book r = q;
        setAndLeak(p, q, r);
    }
}
