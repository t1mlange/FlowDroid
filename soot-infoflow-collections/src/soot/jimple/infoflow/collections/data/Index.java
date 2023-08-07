package soot.jimple.infoflow.collections.data;

/**
 * Represents an index of a list
 */
public class Index extends Location {
    public Index(int paramIdx) {
        super(paramIdx);
    }

    @Override
    public boolean isValueBased() {
        return false;
    }
}
