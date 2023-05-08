package soot.jimple.infoflow.collections;

public class InsertOperation implements ICollectionOperation {
    private final int[] keys;
    private final int data;
    private final boolean returnOldElement;

    public InsertOperation(int[] keys, int data, boolean returnOldElement) {
        this.keys = keys;
        this.data = data;
        this.returnOldElement = returnOldElement;
    }
}
