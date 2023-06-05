package soot.jimple.infoflow.collections.context;

import java.util.HashSet;
import java.util.Set;

import soot.jimple.Constant;
import soot.jimple.infoflow.collections.util.Tristate;

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

	@Override
	public boolean containsInformation() {
		return !keys.isEmpty();
	}

	@Override
	public String toString() {
		return keys.toString();
	}
}
