package soot.jimple.infoflow.collections;

public class RemoveOperation implements ICollectionOperation {
    private final int[] keys;
    private final boolean allKeys;
    private final boolean returnOldElement;

    public RemoveOperation(int[] keys, boolean allKeys, boolean returnOldElement) {
        this.allKeys = allKeys;
        this.keys = allKeys ? null : keys;
        this.returnOldElement = returnOldElement;
    }
}
