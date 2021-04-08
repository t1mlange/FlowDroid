package soot.jimple.infoflow.data;

import soot.Unit;

import java.util.List;

public class UnitWithContext {
    Unit unit;
    List<Unit> context;

    public UnitWithContext(Unit unit, List<Unit> context) {
        this.unit = unit;
        this.context = context;
    }

    public Unit getUnit() {
        return unit;
    }

    public List<Unit> getContext() {
        return context;
    }

    public boolean isIntraprocedural() {
        return context.isEmpty();
    }
}
