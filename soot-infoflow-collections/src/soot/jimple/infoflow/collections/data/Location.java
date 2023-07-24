package soot.jimple.infoflow.collections.data;

/**
 * Represents a location within a container where an element is stored
 */
public abstract class Location {
    private final int paramIdx;
    public Location(int paramIdx) {
        this.paramIdx = paramIdx;
    }

    public int getParamIdx() {
        return paramIdx;
    }

    public abstract boolean isValueBased();
}
