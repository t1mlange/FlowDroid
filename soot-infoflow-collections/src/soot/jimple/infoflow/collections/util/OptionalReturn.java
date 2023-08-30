package soot.jimple.infoflow.collections.util;

public class OptionalReturn<T> {
    private T obj = null;

    public void set(T obj) {
        this.obj = obj;
    }

    public boolean isPresent() {
        return obj != null;
    }

    public T get() {
        return obj;
    }
}
