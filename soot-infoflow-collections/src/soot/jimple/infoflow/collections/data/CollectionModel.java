package soot.jimple.infoflow.collections.data;

import soot.jimple.infoflow.collections.parser.CollectionXMLConstants;

import java.util.Map;

public class CollectionModel {
    public enum CollectionType {
        VALUE_BASED,
        POSITION_BASED
    }

    private final String className;
    private final CollectionType type;
    private final Map<String, CollectionMethod> methods;

    public CollectionModel(String className, String type, Map<String, CollectionMethod> methods) {
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
    }

    public String getClassName() {
        return className;
    }

    public CollectionType getType() {
        return type;
    }

    public boolean hasMethod(String subSig) {
        return methods.containsKey(subSig);
    }

    public CollectionMethod getMethod(String subSig) {
        return methods.get(subSig);
    }
}
