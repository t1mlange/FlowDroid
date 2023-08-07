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
    Tristate intersects(T other);

    /**
     * Check whether this is less or equal to other
     *
     * @param other value-based context
     * @return true on perfect match, maybe on part match and otherwise false
     */
    Tristate lessThanEqual(T other);

    /**
     * Shifts only one bound by n, determined by the sign of n
     *
     * @param n number of shifts
     * @return new position-based context
     */
    T mayShift(int n);

    /**
     * Shifts the position(s) by n
     *
     * @param n number of shifts
     * @return new position-based context
     */
    T exactShift(int n);

    /**
     * Union this with the rotation to receive an over-approximation
     *
     * @param n     distance to be rotated
     * @param bound modulo
     * @return new position-based context
     */
    T mayRotate(T n, T bound);

    /**
     * Rotate the positions by n
     *
     * @param n     distance to be rotated
     * @param bound modulo
     * @return new position-based context
     */
    T exactRotate(T n, T bound);
}
