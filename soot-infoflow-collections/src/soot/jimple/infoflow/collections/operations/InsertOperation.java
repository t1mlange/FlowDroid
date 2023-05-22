package soot.jimple.infoflow.collections.operations;

import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.data.ParamIndex;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;

import java.util.Collection;

public class InsertOperation extends LocationDependentOperation {
    private final int data;

    public InsertOperation(Location[] keys, int data, String field, String fieldType) {
        super(keys, field, fieldType);
        this.data = data;
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getArg(data)))
            return false;

        Value base = iie.getBase();
        ContextDefinition[] ctxt = new ContextDefinition[locations.length];
        for (int i = 0; i < ctxt.length; i++) {
            if (locations[i].getParamIdx() == ParamIndex.LAST_INDEX.toInt())
                ctxt[i] = strategy.getNextPosition(base, stmt);
            else if (locations[i].isValueBased())
                ctxt[i] = strategy.getIndexContext(iie.getArg(locations[i].getParamIdx()), stmt);
            else
                ctxt[i] = strategy.getKeyContext(iie.getArg(locations[i].getParamIdx()), stmt);
        }
        if (strategy.shouldSmash(ctxt))
            ctxt = null;

        AccessPathFragment[] oldFragments = incoming.getAccessPath().getFragments();
        int len = oldFragments == null ? 0 : oldFragments.length;
        AccessPathFragment[] fragments = new AccessPathFragment[len + 1];
        if (oldFragments != null)
            System.arraycopy(oldFragments, 0, fragments, 1, len);
        SootField f = safeGetField(field);
        fragments[0] = new AccessPathFragment(f, f.getType(), ctxt);
        AccessPath ap = manager.getAccessPathFactory().createAccessPath(base, fragments, incoming.getAccessPath().getTaintSubFields());
        out.add(incoming.deriveNewAbstraction(ap, stmt));

        // Insert never removes an element
        return false;
    }
}
