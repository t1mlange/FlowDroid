package soot.jimple.infoflow.collections.operations.forward.alias;

import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.operations.forward.CopyOperation;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Collection;

public class AliasCopyOperation extends CopyOperation {
    public AliasCopyOperation(int from, int to) {
        super(from, to);
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager,
                         IContainerStrategy strategy, Collection<Abstraction> out) {
        return innerApply(to, from, incoming, stmt, manager, out);
    }
}
