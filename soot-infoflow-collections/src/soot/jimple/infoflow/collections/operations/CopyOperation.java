package soot.jimple.infoflow.collections.operations;

import fj.P;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.ParamIndex;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;

import java.util.Collection;

public class CopyOperation implements ICollectionOperation {
    private final int from;
    private final int to;

    public CopyOperation(int from, int to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy keyStrategy, Collection<Abstraction> out) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getArg(from)))
            return false;

        Value value;
        if (to == ParamIndex.BASE.toInt())
            value = iie.getBase();
        else if (to == ParamIndex.RETURN.toInt()) {
            if (!(stmt instanceof AssignStmt))
                return false;
            value = ((AssignStmt) stmt).getLeftOp();
        } else if (to >= 0)
            value = iie.getArg(to);
        else
            throw new RuntimeException("Unexpected to index: " + to);

        AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(incoming.getAccessPath(), value);
        Abstraction abs = incoming.deriveNewAbstraction(ap, stmt);
        if (abs != null)
            out.add(abs);
        return false;
    }
}
