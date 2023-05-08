package soot.jimple.infoflow.collections;

import soot.Value;

public interface IKeyStrategy {
    Tristate intersect(IKey k1, IKey k2);

    IKey getFromValue(Value value);
}
