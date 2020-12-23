package soot.jimple.infoflow.codeOptimization;

import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

import java.util.Collection;

/**
 * There is one special case: If a source stmt is a static method without any parameters,
 * which is called from a static method (see static2Test), the assign stmt s = staticSource() is be the first
 * statement in the iCFG. The CallToReturn flow function only gets applied if a return site is available.
 * In backwards analysis the iCFG is reversed and the assign stmt has no return site. But we need the execution of the
 * CallToReturn flow function to register the arrival at the source.
 *
 * To fix this edge case, we add a NOP as the first statement in the ICFG.
 */
public class addDummyStmt implements ICodeOptimizer {

    @Override
    public void initialize(InfoflowConfiguration config) {

    }

    @Override
    public void run(InfoflowManager manager, Collection<SootMethod> entryPoints, ISourceSinkManager sourcesSinks, ITaintPropagationWrapper taintWrapper) {
        for (SootMethod entryPoint : entryPoints) {
            System.out.println(entryPoint.toString());
        }
    }
}
