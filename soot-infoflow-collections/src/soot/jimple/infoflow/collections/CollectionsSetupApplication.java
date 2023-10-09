package soot.jimple.infoflow.collections;

import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.collections.problems.rules.CollectionRulePropagationManagerFactory;
import soot.jimple.infoflow.collections.solver.fastSolver.WideningCollectionInfoflowSolver;
import soot.jimple.infoflow.collections.strategies.widening.WideningOnRevisitStrategy;
import soot.jimple.infoflow.ipc.IIPCManager;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Class that sets up the correct classes for analysis with precise collection handling
 */
public class CollectionsSetupApplication extends SetupApplication {
    public CollectionsSetupApplication(InfoflowAndroidConfiguration config) {
        super(config);
        commonInit();
    }

    public CollectionsSetupApplication(String androidJar, String apkFileLocation) {
        super(androidJar, apkFileLocation);
        commonInit();
    }

    public CollectionsSetupApplication(String androidJar, String apkFileLocation, IIPCManager ipcManager) {
        super(androidJar, apkFileLocation, ipcManager);
        commonInit();
    }

    public CollectionsSetupApplication(InfoflowAndroidConfiguration config, IIPCManager ipcManager) {
        super(config, ipcManager);
        commonInit();
    }

    protected class CollectionsInPlaceInfoflow extends InPlaceInfoflow {
        public CollectionsInPlaceInfoflow(String androidPath, boolean forceAndroidJar, BiDirICFGFactory icfgFactory, Collection<SootMethod> additionalEntryPointMethods) {
            super(androidPath, forceAndroidJar, icfgFactory, additionalEntryPointMethods);
        }

        @Override
        protected void performCodeInstrumentationBeforeDCE(InfoflowManager manager,
                                                           Set<SootMethod> excludedMethods) {
            super.performCodeInstrumentationBeforeDCE(manager, excludedMethods);
            StringResourcesResolver res = new StringResourcesResolver();
            res.initialize(manager.getConfig());
            res.run(manager, excludedMethods, manager.getSourceSinkManager(), manager.getTaintWrapper());
        }

        @Override
        protected IPropagationRuleManagerFactory initializeRuleManagerFactory() {
            return new CollectionRulePropagationManagerFactory();
        }

//        @Override
//        protected IInfoflowSolver createDataFlowSolver(InterruptableExecutor executor,
//                                                       AbstractInfoflowProblem problem, InfoflowConfiguration.SolverConfiguration solverConfig) {
//            WideningCollectionInfoflowSolver solver = new WideningCollectionInfoflowSolver(problem, executor);
//            solver.setWideningStrategy(new WideningOnRevisitStrategy(manager, Collections.singleton("void add(int,java.lang.Object)")));
//            solverPeerGroup.addSolver(solver);
//            return solver;
//        }
    }

    private void commonInit() {
        addOptimizationPass(new SetupApplication.OptimizationPass() {
            @Override
            public void performCodeInstrumentationBeforeDCE(InfoflowManager manager, Set<SootMethod> excludedMethods) {
                StringResourcesResolver res = new StringResourcesResolver();
                res.initialize(manager.getConfig());
                res.run(manager, excludedMethods, manager.getSourceSinkManager(), manager.getTaintWrapper());
            }

            @Override
            public void performCodeInstrumentationAfterDCE(InfoflowManager manager, Set<SootMethod> excludedMethods) {

            }
        });
    }

    protected IInPlaceInfoflow createInfoflowInternal(Collection<SootMethod> lifecycleMethods) {
        final String androidJar = config.getAnalysisFileConfig().getAndroidPlatformDir();
        return new CollectionsInPlaceInfoflow(androidJar, forceAndroidJar, cfgFactory, lifecycleMethods);
    }
}
