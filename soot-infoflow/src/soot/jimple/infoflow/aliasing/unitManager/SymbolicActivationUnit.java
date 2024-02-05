package soot.jimple.infoflow.aliasing.unitManager;

import soot.SootMethod;
import soot.Unit;
import soot.UnitPrinter;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.sourcesSinks.manager.BaseSourceSinkManager;
import soot.jimple.internal.JNopStmt;

import java.util.Objects;
import java.util.Set;

/**
 * Symbolic activation unit per Gui et al. (2023): Merge-Replay: Efficient IFDS-Based Taint Analysis
 * by Consolidating Equivalent Value Flows
 */
public class SymbolicActivationUnit extends JNopStmt {
    // Global Activation Unit
    public static final SymbolicActivationUnit GAU = new SymbolicActivationUnit();

    // For each caller, callee, abstractions at most one symbolic activation unit exists
    private final SootMethod caller;
    private final SootMethod callee;
    private final Abstraction abstraction;

    // Concrete activation units that should be replayed for this symbolic unit
    private final Set<Unit> concreteUnits;

    private final int hashCode;

    private SymbolicActivationUnit() {
        caller = null;
        callee = null;
        abstraction = null;
        concreteUnits = null;
        hashCode = 0;
    }

    public SymbolicActivationUnit(SootMethod caller, SootMethod callee, Abstraction abstraction) {
        this.caller = caller;
        this.callee = callee;
        this.abstraction = abstraction;
        this.concreteUnits = new ConcurrentHashSet<>();
        this.hashCode = Objects.hash(caller, callee, abstraction);
    }

    public boolean matchesContext(SootMethod caller, SootMethod callee) {
        return this.caller == caller && this.callee == callee;
    }

    public boolean addConcreteUnit(Unit u) {
        assert !(u instanceof SymbolicActivationUnit);
        return this.concreteUnits.add(u);
    }

    public Set<Unit> getConcreteUnits() {
        return this.concreteUnits;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        SymbolicActivationUnit that = (SymbolicActivationUnit) object;
        return hashCode == that.hashCode && Objects.equals(caller, that.caller)
                && Objects.equals(callee, that.callee) && Objects.equals(abstraction, that.abstraction);
    }

    @Override
    public void toString(UnitPrinter up) {
        up.literal(this.toString());
    }

    @Override
    public String toString() {
        return this == GAU ? "GAU" : String.format("SYM<%s, %s>", caller.getName(), callee.getName());
    }
}
