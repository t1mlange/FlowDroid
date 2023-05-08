package soot.jimple.infoflow.collections;

import java.util.List;

public class CollectionMethod {
    private final String subSig;
    private final List<ICollectionOperation> operations;

    public CollectionMethod(String subSig, List<ICollectionOperation> operations) {
        this.subSig = subSig;
        this.operations = operations;
    }
}
