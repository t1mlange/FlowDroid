package soot.jimple.infoflow.collections.data;

/**
 * Mapping of ints to parameters or base objects
 */
public final class ParamIndex {
    // Represents an index not used
    public static final int UNUSED = -42;
    // Represents that the location isn't read but part of the result
    public static final int COPY = -6;
    // Represents any index
    public static final int ALL = -5;
    // Represents the first index (only for position based)
    public static final int FIRST_INDEX = -4;
    // Represents the last index (only for position based)
    public static final int LAST_INDEX = -3;
    // Represents that the access is the data argument
    public static final int RETURN = -2;
    // Represents the base object
    public static final int BASE = -1;
}
