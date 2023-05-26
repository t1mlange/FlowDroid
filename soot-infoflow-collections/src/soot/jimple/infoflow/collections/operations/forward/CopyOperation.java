package soot.jimple.infoflow.collections.operations.forward;

import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.ParamIndex;
import soot.jimple.infoflow.collections.operations.ICollectionOperation;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;

import java.util.Collection;

public class CopyOperation implements ICollectionOperation {
    protected final int from;
    protected final int to;

    public CopyOperation(int from, int to) {
        this.from = from;
        this.to = to;
    }

    protected boolean innerApply(int from, int to, Abstraction incoming, Stmt stmt, InfoflowManager manager, Collection<Abstraction> out) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getArg(from)))
            return false;

        Value value;
        switch (to) {
            case ParamIndex.BASE:
                value = iie.getBase();
                break;
            case ParamIndex.RETURN:
                if (!(stmt instanceof AssignStmt))
                    return false;
                value = ((AssignStmt) stmt).getLeftOp();
                break;
            default:
                if (to < 0)
                    throw new RuntimeException("Unexpected to index: " + to);
                value = iie.getArg(to);
        }

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
