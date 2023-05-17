package soot.jimple.infoflow.collections.operations;

import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;

import java.util.Collection;

public class ReturnOperation implements ICollectionOperation {
    private final int data;

    public ReturnOperation(int data) {
        this.data = data;
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy keyStrategy, Collection<Abstraction> out) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getArg(data))
                || !(stmt instanceof AssignStmt))
            return false;

        AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(incoming.getAccessPath(),
                ((AssignStmt) stmt).getLeftOp());
        Abstraction abs = incoming.deriveNewAbstraction(ap, stmt);
        if (abs != null)
            out.add(abs);
        return false;
    }
}
