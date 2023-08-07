package soot.jimple.infoflow.collections.operations.forward;

import java.util.Collection;

import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.operations.AbstractOperation;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;

public class CopyOperation extends AbstractOperation {

    protected final int from;
    protected final int to;

    public CopyOperation(int from, int to) {
        this.from = from;
        this.to = to;
    }

    protected boolean innerApply(int from, int to, Abstraction incoming, Stmt stmt, InfoflowManager manager, Collection<Abstraction> out) {
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), getValueFromIndex(from, stmt)))
            return false;

        Value value = getValueFromIndex(to, stmt);
        AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(incoming.getAccessPath(), value);
        Abstraction abs = incoming.deriveNewAbstraction(ap, stmt);
        if (abs != null)
            out.add(abs);
        return false;
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager,
                         IContainerStrategy strategy, Collection<Abstraction> out) {
        return innerApply(from, to, incoming, stmt, manager, out);
    }
}
