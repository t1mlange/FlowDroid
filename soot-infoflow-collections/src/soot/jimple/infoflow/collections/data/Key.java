package soot.jimple.infoflow.collections.data;

/**
 * Represents a key of a map
 */
public class Key extends Location {
    public Key(int paramIdx) {
        super(paramIdx);
    }

    @Override
    public boolean isValueBased() {
        return false;
    }
}
