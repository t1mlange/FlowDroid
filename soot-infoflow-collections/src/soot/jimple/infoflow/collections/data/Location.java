package soot.jimple.infoflow.collections.data;

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
