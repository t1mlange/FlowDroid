package soot.jimple.infoflow.collections.data;

import java.util.List;

import soot.jimple.infoflow.collections.operations.ICollectionOperation;

public class CollectionMethod {
	private final String sig;
	private final String subSig;
	private final ICollectionOperation[] operations;
	private final ICollectionOperation[] aliasOperations;

	public CollectionMethod(String className, String subSig, List<ICollectionOperation> operations,
			List<ICollectionOperation> aliasOperations) {
		this.sig = "<" + className + ": " + subSig + ">";
		this.subSig = subSig;
		this.operations = operations.toArray(new ICollectionOperation[0]);
		this.aliasOperations = aliasOperations.toArray(new ICollectionOperation[0]);
	}

	public String getSignature() {
		return sig;
	}

	public String getSubSignature() {
		return subSig;
	}

	public ICollectionOperation[] operations() {
		return operations;
	}

	public ICollectionOperation[] aliasOperations() {
		return aliasOperations;
	}

	@Override
	public String toString() {
		return "CollectionMethod(" + subSig + ")";
	}
}
