package soot.jimple.infoflow.collections.context;

import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.HashSet;
import java.util.Set;

public class IntervalContext implements ContextDefinition {
    private final int min;
    private final int max;

    private final Set<Stmt> shiftStmts;

    public IntervalContext(int i) {
        min = i;
        max = i;
        shiftStmts = new HashSet<>();
    }

    public IntervalContext(int min, int max, Set<Stmt> shiftStmts, Stmt stmt) {
        this.min = min;
        this.max = max;
        this.shiftStmts = new HashSet<>(shiftStmts);
        this.shiftStmts.add(stmt);
    }

    public Tristate intersect(IntervalContext other) {
        if (this.equals(other))
            return Tristate.TRUE();
        if (Integer.max(min, other.min) <= Integer.min(max, other.max))
            return Tristate.MAYBE();
        return Tristate.FALSE();
    }

    public ContextDefinition shiftRight(Stmt stmt) {
        if (shiftStmts.contains(stmt))
            return UnknownContext.v();

        if (max < Integer.MAX_VALUE)
            return new IntervalContext(min + 1, max + 1, shiftStmts, stmt);
        return this;
    }

    public ContextDefinition addRight(Stmt stmt) {
        if (shiftStmts.contains(stmt))
            return UnknownContext.v();

        if (max < Integer.MAX_VALUE)
            return new IntervalContext(min, max + 1, shiftStmts, stmt);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntervalContext other = (IntervalContext) o;
        return min == other.min && max == other.max;
    }

    @Override
    public String toString() {
        return min + "-" + max;
    }
}
