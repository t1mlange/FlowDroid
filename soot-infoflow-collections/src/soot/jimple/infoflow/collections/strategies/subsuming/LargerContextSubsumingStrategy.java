package soot.jimple.infoflow.collections.strategies.subsuming;

import java.util.Arrays;
import java.util.Set;

import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.IntervalContext;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

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

    @Override
    public Abstraction applyDiffOf(Abstraction d1, Abstraction d2, Abstraction targetVal) {
        // We have to talk about the same abstraction
        assert d1.getAccessPath().getPlainValue() == targetVal.getAccessPath().getPlainValue()
                && d1.getAccessPath().getFragmentCount() == targetVal.getAccessPath().getFragmentCount();

        AccessPath d2Ap = d2.getAccessPath();
        if (d2Ap.getFragmentCount() == 0) {
            return targetVal.deriveNewAbstraction(d2Ap, null);
        }

        AccessPath targetAp = targetVal.getAccessPath();
        int maxIdx = targetAp.getFragmentCount() - 1;
        AccessPathFragment[] newFragments = new AccessPathFragment[d2Ap.getFragmentCount()];
        for (int i = d2Ap.getFragmentCount() - 1; i >= 0; i--) {
            AccessPathFragment f = d2Ap.getFragments()[i];
            ContextDefinition[] ctxt = f.getContext();
            // Only if the fragment at the end has a context, we have to look for replacements, because...
            // case 1) there was a context dependent statement on the flow, then this method will never be called
            // case 2) the deref of a context-dependent field is independent of the key (i.e. iterator)
            if (ctxt != null) {
                // We might have to replace the context
                for (int j = maxIdx; j >= 0; j--) {
                    AccessPathFragment targetF = targetAp.getFragments()[j];
                    if (targetF.getField().equals(f.getField())) {
                        ctxt = targetF.getContext();
                        maxIdx = j;
                        break;
                    }
                }
            }
            newFragments[i] = f.copyWithNewContext(ctxt);
        }
        AccessPath diffAp = manager.getAccessPathFactory().createAccessPath(targetAp.getPlainValue(), newFragments, targetAp.getTaintSubFields());
        assert diffAp != null;
        return targetVal.deriveNewAbstraction(diffAp, null);
    }
}
