package soot.jimple.infoflow.aliasing.unitManager;

import soot.SootMethod;
import soot.Unit;
import soot.UnitPrinter;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.data.Abstraction;
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

    private final SootMethod caller;
    private final SootMethod callee;

    private final Abstraction abstraction;

    private final Set<Unit> targets;

    private final int hashCode;

    private SymbolicActivationUnit() {
        caller = null;
        callee = null;
        abstraction = null;
        targets = null;
        hashCode = 0;
    }

    public SymbolicActivationUnit(SootMethod caller, SootMethod callee, Abstraction abstraction) {
        this.caller = caller;
        this.callee = callee;
        this.abstraction = abstraction;
        this.targets = new ConcurrentHashSet<>();
        this.hashCode = Objects.hash(caller, callee, abstraction);
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
