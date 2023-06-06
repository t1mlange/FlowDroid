package soot.jimple.infoflow.collections.data;

import java.util.List;

import soot.jimple.infoflow.collections.operations.ICollectionOperation;

public class CollectionMethod {
    private final String subSig;
    private final ICollectionOperation[] operations;
    private final ICollectionOperation[] aliasOperations;

    public CollectionMethod(String subSig, List<ICollectionOperation> operations,
                            List<ICollectionOperation> aliasOperations) {
        this.subSig = subSig;
        this.operations = operations.toArray(new ICollectionOperation[0]);
        this.aliasOperations = aliasOperations.toArray(new ICollectionOperation[0]);
    }

    public ICollectionOperation[] operations() {
        return operations;
    }

    public ICollectionOperation[] aliasOperations() {
        return aliasOperations;
    }
}
