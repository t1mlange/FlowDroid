package soot.jimple.infoflow.collections.strategies.widening;

import soot.Unit;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;

public abstract class AbstractWidening implements WideningStrategy<Unit, Abstraction> {
    protected final InfoflowManager manager;

    protected AbstractWidening(InfoflowManager manager) {
        this.manager = manager;
    }

    @Override
    public Abstraction forceWiden(Abstraction abs, Unit unit) {
        AccessPathFragment[] oldFragments = abs.getAccessPath().getFragments();
        AccessPathFragment[] fragments = new AccessPathFragment[oldFragments.length];
        System.arraycopy(oldFragments, 1, fragments, 1, fragments.length - 1);
        fragments[0] = oldFragments[0].copyWithNewContext(null);
        AccessPath ap = manager.getAccessPathFactory().createAccessPath(abs.getAccessPath().getPlainValue(), fragments,
                abs.getAccessPath().getTaintSubFields());
        return abs.deriveNewAbstraction(ap, null);
    }
}
