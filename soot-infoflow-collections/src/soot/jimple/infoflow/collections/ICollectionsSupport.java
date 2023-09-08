package soot.jimple.infoflow.collections;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Set;

public interface ICollectionsSupport {
    Set<Abstraction> getTaintsForMethodApprox(Stmt stmt, Abstraction d1, Abstraction incoming);
}
