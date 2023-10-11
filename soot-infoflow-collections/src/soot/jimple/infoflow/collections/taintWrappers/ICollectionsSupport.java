package soot.jimple.infoflow.collections.taintWrappers;

import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

import java.util.Set;

public interface ICollectionsSupport extends ITaintPropagationWrapper {
    Set<Abstraction> getTaintsForMethodApprox(Stmt stmt, Abstraction d1, Abstraction incoming);

    IContainerStrategy getContainerStrategy();
}
