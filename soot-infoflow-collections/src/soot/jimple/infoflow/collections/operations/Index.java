package soot.jimple.infoflow.collections.operations;

/**
 * Maps special indices to ints
 */
public enum Index {
    // Represents an index not used
    UNUSED(-42),
    // Represents any index
    ALL(-2),
    // Represents the last index (only for position based)
    LAST_INDEX(-1);

    private final int value;

    Index(int value) {
        this.value = value;
    }

    public int toInt() {
        return value;
    }
}
