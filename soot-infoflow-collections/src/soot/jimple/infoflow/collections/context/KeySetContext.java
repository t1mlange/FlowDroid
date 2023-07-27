package soot.jimple.infoflow.collections.context;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import soot.jimple.Constant;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

public class KeySetContext implements ValueBasedContext {
	Set<Constant> keys;

	public KeySetContext(Constant key) {
		this.keys = new HashSet<>();
		this.keys.add(key);
	}

	public KeySetContext(Set<Constant> keys) {
		this.keys = keys;
	}

	public Tristate intersect(KeySetContext other) {
		boolean all = true;
		boolean any = false;
		for (Constant c : this.keys) {
			if (other.keys.contains(c))
				any = true;
			else
				all = false;

			if (any && !all)
				return Tristate.MAYBE();
		}

		return Tristate.fromBoolean(any && all);
	}

	public boolean entails(ContextDefinition other) {
		if (!(other instanceof KeySetContext))
			return false;

		for (Constant key : keys)
			if (!((KeySetContext) other).keys.contains(key))
				return false;

		return true;
	}

	@Override
	public boolean containsInformation() {
		return !keys.isEmpty();
	}

	@Override
	public String toString() {
		return keys.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KeySetContext that = (KeySetContext) o;
		return Objects.equals(keys, that.keys);
	}

	@Override
	public int hashCode() {
		return Objects.hash(keys);
	}
}
