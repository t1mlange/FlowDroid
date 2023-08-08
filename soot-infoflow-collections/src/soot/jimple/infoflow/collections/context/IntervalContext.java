package soot.jimple.infoflow.collections.context;

import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Objects;

public class IntervalContext implements PositionBasedContext<IntervalContext> {
	private final int min;
	private final int max;

	public IntervalContext(int i) {
		assert i >= 0;

		min = i;
		max = i;
	}

	public IntervalContext(int min, int max) {
		assert min >= 0;
		assert max >= 0;

		this.min = min;
		this.max = max;
	}

	@Override
	public Tristate intersects(IntervalContext other) {
		if (this.equals(other))
			return Tristate.TRUE();
		if (Integer.max(min, other.min) <= Integer.min(max, other.max))
			return Tristate.MAYBE();
		return Tristate.FALSE();
	}

	@Override
	public boolean entails(ContextDefinition obj) {
		if (!(obj instanceof IntervalContext))
			return false;
		IntervalContext other = (IntervalContext) obj;
		return this.min <= other.min && other.max <= this.max;
	}

	@Override
	public IntervalContext exactShift(IntervalContext n) {
		// Never increase the upper bound above the max value
		int newMax = (n.max > 0 && Integer.MAX_VALUE - max < n.max) ? Integer.MAX_VALUE : max + n.max;
		int newMin = min + n.min;
		if (newMin < 0) {
			// We cannot have indices less than zero
			if (newMax < 0)
				return null;
			// If the max is above zero, keep the minimum at zero
			newMin = 0;
		}
		return new IntervalContext(newMin, newMin);
	}

	@Override
	public IntervalContext mayShift(IntervalContext n) {
		// If the shift shifts the lower bound, update it
		int newMin = n.min < 0 ? Math.max(min + n.min, 0) : min;
		// If the shift shifts the upper bound, update it
		int newMax = n.max > 0 ? Math.min(max + n.max, Integer.MAX_VALUE) : max;

		return new IntervalContext(newMin, newMax);
	}

	@Override
	public IntervalContext exactRotate(IntervalContext n, IntervalContext bound) {
		int min1 = (min + n.min) % bound.min;
		int min2 = (min + n.min) % bound.max;
		int max1 = (max + n.max) % bound.min;
		int max2 = (max + n.max) % bound.max;
		return new IntervalContext(Math.min(min1, min2), Math.max(max1, max2));
	}

	@Override
	public IntervalContext mayRotate(IntervalContext n, IntervalContext bound) {
		return this.union(exactRotate(n, bound));
	}

	private IntervalContext union(IntervalContext other) {
		return new IntervalContext(Math.min(this.min, other.min), Math.max(this.max, other.max));
	}

	@Override
	public Tristate lessThanEqual(IntervalContext other) {
		if (max <= other.min)
			return Tristate.TRUE();
		if (other.max <= min)
			return Tristate.FALSE();
		return Tristate.MAYBE();
	}

	public boolean containsInformation() {
		return min != 0 || max != Integer.MAX_VALUE;
	}

	public int size() {
		return max - min;
	}

	@Override
	public int hashCode() {
		return Objects.hash(min, max);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		IntervalContext other = (IntervalContext) o;
		return min == other.min && max == other.max;
	}

	@Override
	public String toString() {
		return min + "-" + max;
	}
}
