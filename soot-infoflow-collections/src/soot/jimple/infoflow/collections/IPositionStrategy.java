package soot.jimple.infoflow.collections;

public interface IPositionStrategy extends IKeyStrategy {
    Tristate lessThan(IPosition p1, IPosition p2);
}
