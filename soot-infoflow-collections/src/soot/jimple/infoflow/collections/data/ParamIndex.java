package soot.jimple.infoflow.collections.data;

/**
 * Maps special indices to ints
 */
public enum ParamIndex {
    // Represents an index not used
    UNUSED(-42),
    // Represents the base object
    // Represents any index
    ALL(-4),
    // Represents that the access is the data argument
    RETURN(-3),
    BASE(-2),
    // Represents the last index (only for position based)
    LAST_INDEX(-1);

    private final int value;

    ParamIndex(int value) {
        this.value = value;
    }

    public int toInt() {
        return value;
    }
}
