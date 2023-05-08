package soot.jimple.infoflow.collections;

import java.util.Arrays;

public class AccessOperation implements ICollectionOperation {
    private final int[] keys;

    public AccessOperation(int[] keys) {
        this.keys = keys;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessOperation that = (AccessOperation) o;
        return Arrays.equals(keys, that.keys);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(keys);
    }
}
