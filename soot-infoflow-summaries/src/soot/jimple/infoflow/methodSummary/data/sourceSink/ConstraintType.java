package soot.jimple.infoflow.methodSummary.data.sourceSink;

public enum ConstraintType {
    TRUE,
    FALSE,
    SHIFT_RIGHT,
    SHIFT_LEFT,
    // Tells to keep the constraint from the source field
    KEEP,
    // Tells to keep the constraint from the source field only
    // if the rhs is used in a read-only fashion
    READONLY
}
