package soot.jimple.infoflow.collections.data;

import java.util.Collection;
import java.util.Map;

import soot.FastHierarchy;
import soot.Scene;
import soot.SootClass;
import soot.jimple.infoflow.collections.parser.CollectionXMLConstants;

public class CollectionModel {
	public enum CollectionType {
		VALUE_BASED, POSITION_BASED
	}

	private final String className;
	private final CollectionType type;

	private SootClass[] excludedSubclasses;
	private String[] excludedSubclassesStrings;

	private Map<String, CollectionMethod> methods;

	public CollectionModel(String className, String type, Map<String, CollectionMethod> methods, String[] excludedSubclasses) {
		this.className = className;
		switch (type) {
		case CollectionXMLConstants.VALUE:
			this.type = CollectionType.VALUE_BASED;
			break;
		case CollectionXMLConstants.POSITION:
			this.type = CollectionType.POSITION_BASED;
			break;
		default:
			throw new RuntimeException("Unknown type string: " + type);
		}
		this.methods = methods;
		this.excludedSubclassesStrings = excludedSubclasses;
	}

	public void initialize() {
		if (excludedSubclassesStrings != null) {
			excludedSubclasses = new SootClass[excludedSubclassesStrings.length];
			for (int i = 0; i < excludedSubclasses.length; i++) {
				SootClass sc = Scene.v().getSootClassUnsafe(excludedSubclassesStrings[i]);
				if (sc != null)
					excludedSubclasses[i] = sc;
			}
			excludedSubclassesStrings = null;
		}
	}

	public String getClassName() {
		return className;
	}

	public CollectionType getType() {
		return type;
	}

	public boolean hasMethod(String subsig) {
		return methods.containsKey(subsig);
	}

	public CollectionMethod getMethod(String subsig) {
		return methods.get(subsig);
	}

	public boolean isNotExcluded(SootClass sc, FastHierarchy fh) {
		if (excludedSubclasses == null)
			return true;

		for (SootClass ex : excludedSubclasses)
			if (fh.isSubclass(sc, ex))
				return false;
		return true;
	}

	public Collection<CollectionMethod> getAllMethods() {
		return methods.values();
	}

	@Override
	public String toString() {
		return "CollectionModel(" + className + ")";
	}
}
