package soot.jimple.infoflow.collections.context;

import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

public class IntervalContext implements PositionBasedContext {
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

	public Tristate intersect(IntervalContext other) {
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
		return other.min <= this.min && this.max < other.max;
	}

	public ContextDefinition shiftRight(Stmt stmt) {
		if (max < Integer.MAX_VALUE)
			return new IntervalContext(min + 1, max + 1);
		return this;
	}

	public ContextDefinition addRight(Stmt stmt) {
		if (max < Integer.MAX_VALUE)
			return new IntervalContext(min, max + 1);
		return this;
	}

	public ContextDefinition shiftLeft(Stmt stmt) {
		if (min == 0)
			return null;

		if (max < Integer.MAX_VALUE)
			return new IntervalContext(min - 1, max - 1);
		return this;
	}

	public ContextDefinition subtractLeft(Stmt stmt) {
		if (max < Integer.MAX_VALUE)
			return new IntervalContext(min - 1, max);
		return this;
	}

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
