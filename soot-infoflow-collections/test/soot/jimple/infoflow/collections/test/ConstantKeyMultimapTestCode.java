package soot.jimple.infoflow.collections.test;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class ConstantKeyMultimapTestCode {
    class A {
        String str;
        String unrelated;
    }

    public void testMultimapPutGet1() {
        Multimap<String, A> mmap = HashMultimap.create();
        A a = new A();
        a.str = source();
        mmap.put("K", a);
        Collection<A> c = mmap.get("K");
        sink(c.iterator().next().str);
    }

    public void testMultimapPutGet2() {
        Multimap<String, A> mmap = HashMultimap.create();
        A a = new A();
        a.str = source();
        mmap.put("K", a);
        Collection<A> c = mmap.get("K");
        sink(c.iterator().next().unrelated);
    }

    public void testMultimapPutGet3() {
        Multimap<String, A> mmap = HashMultimap.create();
        A a = new A();
        a.str = source();
        mmap.put("K", a);
        Collection<A> c = mmap.get("L");
        sink(c.iterator().next().str);
    }
}
