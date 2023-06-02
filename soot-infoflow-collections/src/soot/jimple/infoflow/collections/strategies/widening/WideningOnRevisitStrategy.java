package soot.jimple.infoflow.collections.strategies.widening;

import soot.Unit;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.context.PositionBasedContext;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;
import soot.util.ConcurrentHashMultiMap;

/**
 * Widens each fact that revisits a statement
 *
 * @author Tim Lange
 */
public class WideningOnRevisitStrategy implements WideningStrategy<Unit, Abstraction> {
    private final InfoflowManager manager;

    private final ConcurrentHashMultiMap<Unit, Abstraction> seenAbstractions;

    public WideningOnRevisitStrategy(InfoflowManager manager) {
        this.manager = manager;
        this.seenAbstractions = new ConcurrentHashMultiMap<>();
    }

    private boolean isWideningCandidate(Abstraction abs) {
        if (abs.getAccessPath().getFragmentCount() == 0)
            return false;

        AccessPathFragment fragment = abs.getAccessPath().getFirstFragment();
        if (!fragment.hasContext())
            return false;

        for (ContextDefinition ctxt : fragment.getContext())
            if (ctxt instanceof PositionBasedContext)
                return true;

        return false;
    }

    @Override
    public void recordNewFact(Abstraction fact, Unit u) {
        if (isWideningCandidate(fact))
            seenAbstractions.put(u, fact);
    }

    @Override
    public Abstraction widen(Abstraction abs, Unit u) {
        if (!seenAbstractions.contains(u, abs))
            return abs;

        AccessPathFragment[] oldFragments = abs.getAccessPath().getFragments();
        AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length];
        System.arraycopy(oldFragments, 1, fragments, 1, fragments.length - 1);
        fragments[0] = oldFragments[0].copyWithNewContext(null);
        AccessPath ap = manager.getAccessPathFactory().createAccessPath(abs.getAccessPath().getPlainValue(),
                fragments, abs.getAccessPath().getTaintSubFields());
        return abs.deriveNewAbstraction(ap, null);
    }
}
