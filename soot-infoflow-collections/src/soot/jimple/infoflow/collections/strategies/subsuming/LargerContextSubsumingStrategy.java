package soot.jimple.infoflow.collections.strategies.subsuming;

import heros.solver.Pair;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Arrays;
import java.util.Set;

public class LargerContextSubsumingStrategy implements SubsumingStrategy<Unit, Abstraction> {
    private final InfoflowManager manager;
    private final Set<String> methods;

    public LargerContextSubsumingStrategy(InfoflowManager manager, Set<String> methods) {
        this.manager = manager;
        this.methods = methods;
    }

    protected int getContextSize(Abstraction abs) {
        if (abs.getAccessPath().getFragmentCount() == 0)
            return 0;

        int size = 0;
        for (AccessPathFragment f : abs.getAccessPath().getFragments())
            if (f.hasContext())
                for (ContextDefinition c : f.getContext())
                    size += c instanceof IntervalContext ? ((IntervalContext) c).size() : 1;

        return -size;
    }

    @Override
    public int compare(Abstraction a, Abstraction b) {
        // Prioritize empty contexts and then larger contexts
        int pA = getContextSize(a);
        if (pA == 0)
            return -1;
        int pB = getContextSize(b);
        if (pB == 0)
            return -1;
        if (pA <= pB)
            return -1;
        return 1;
    }

    @Override
    public boolean subsumes(Abstraction a, Abstraction b) {
        return b.entails(a);
    }

    @Override
    public boolean hasContext(Abstraction abs) {
        return abs.getAccessPath().getFragmentCount() != 0 && Arrays.stream(abs.getAccessPath().getFragments()).anyMatch(f -> f.hasContext());
    }

    @Override
    public Abstraction removeContext(Abstraction abs) {
        AccessPathFragment[] oldFragments = abs.getAccessPath().getFragments();
        AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length];
        for (int i = 0; i < fragments.length; i++) {
            fragments[i] = oldFragments[i].copyWithNewContext(null);
        }
        AccessPath ap = manager.getAccessPathFactory().createAccessPath(abs.getAccessPath().getPlainValue(), fragments,
                abs.getAccessPath().getTaintSubFields());
        return abs.deriveNewAbstraction(ap, null);
    }

    @Override
    public Abstraction chooseContext(Set<Abstraction> availableContexts, Abstraction currentContext) {
        Abstraction curr = null;
        for (Abstraction sumD1 : availableContexts) {
            // We don't have a summary, so we need a coarser summary...
            if (sumD1.entails(currentContext)) {
                // ...but from all coarser summaries, we want the closest to the current context
                if (curr == null || curr.entails(sumD1))
                    curr = sumD1;
            }
        }

        return curr;
    }

    @Override
    public boolean affectsContext(Abstraction incoming, Abstraction outgoing) {
        AccessPath inAp = incoming.getAccessPath();
        AccessPath outAp = outgoing.getAccessPath();

        int next = 0;
        for (int i = 0; i < inAp.getFragmentCount(); i++) {
            AccessPathFragment fIn = inAp.getFragments()[i];
            // No context = nothing to check
            if (!fIn.hasContext())
                continue;

            boolean found = false;
            for (int j = next; j < outAp.getFragmentCount(); j++) {
                if (fIn.equals(outAp.getFragments()[j])) {
                    found = true;
                    // Make sure the order of fragments is preserved
                    next = j + 1;
                    break;
                }
            }

            // If we cannot find the contexts in the same order
            // this statement affected the context
            if (!found)
                return true;
        }

        return false;
    }

    @Override
    public boolean affectsContext(Unit unit) {
        Stmt stmt = (Stmt) unit;
        return stmt.containsInvokeExpr() && methods.contains(stmt.getInvokeExpr().getMethod().getSubSignature());
    }
}
