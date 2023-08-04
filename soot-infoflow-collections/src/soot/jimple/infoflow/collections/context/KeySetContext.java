package soot.jimple.infoflow.collections.context;

import java.util.Objects;
import java.util.Set;

import soot.jimple.infoflow.collections.util.ImmutableArraySet;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

/**
 * Representation of map keys using a set possible keys
 *
 * @param <C> key type
 */
public class KeySetContext<C> implements ValueBasedContext<KeySetContext<?>> {
	private final Set<C> keys;

	public KeySetContext(C key) {
		this.keys = new ImmutableArraySet<>(key);
	}

	public KeySetContext(Set<C> keys) {
		this.keys = new ImmutableArraySet<>(keys);
	}

	@Override
	public Tristate intersect(KeySetContext<?> other) {
		boolean all = true;
		boolean any = false;
		for (C c : this.keys) {
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

		for (C key : keys)
			if (!((KeySetContext<?>) other).keys.contains(key))
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
		KeySetContext<?> that = (KeySetContext<?>) o;
		return Objects.equals(keys, that.keys);
	}

	@Override
	public int hashCode() {
		return Objects.hash(keys);
	}
}
