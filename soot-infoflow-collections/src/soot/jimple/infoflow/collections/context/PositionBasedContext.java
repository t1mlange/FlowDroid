package soot.jimple.infoflow.collections.context;

import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

/**
 * Interface for all position-based contexts.
 * Immutable by default.
 *
 * @param <T> concrete position-based context type
 */
public interface PositionBasedContext<T extends PositionBasedContext<?>> extends ContextDefinition {
    /**
     * Check whether this and other intersects
     *
     * @param other value-based context
     * @return true on perfect match, maybe on part match and otherwise false
     */
    Tristate intersect(T other);

    /**
     * Check whether this is less or equal to other
     *
     * @param other value-based context
     * @return true on perfect match, maybe on part match and otherwise false
     */
    Tristate lessThanEqual(T other);

    /**
     * Subtracts from the lower bound
     *
     * @return new position-based context
     */
    T subtractLeft();

    /**
     * Shifts the position(s) one to the left (i.e. decreases)
     *
     * @return new position-based context
     */
    T shiftLeft();

    /**
     * Adds one to upper bound
     *
     * @return new position-based context
     */
    T addRight();

    /**
     * Shifts the position(s) one to the right (i.e. increases)
     *
     * @return new position-based context
     */
    T shiftRight();
}
