package soot.jimple.infoflow.collections.operations;

import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;

public abstract class AbstractOperation implements ICollectionOperation {
    protected boolean matchesBaseObject(Stmt stmt, Abstraction taintedPath, InfoflowManager manager) {
        if (!stmt.containsInvokeExpr())
            return false;
        InvokeExpr ie = stmt.getInvokeExpr();
        if (!(ie instanceof InstanceInvokeExpr))
            return false;

        Value base = ((InstanceInvokeExpr) ie).getBase();
        return manager.getAliasing().mayAlias(taintedPath.getAccessPath(), base) != null;
    }
}
