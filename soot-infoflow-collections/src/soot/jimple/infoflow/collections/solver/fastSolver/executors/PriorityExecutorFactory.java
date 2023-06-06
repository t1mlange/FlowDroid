package soot.jimple.infoflow.collections.solver.fastSolver.executors;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.executors.SetPoolExecutor;
import soot.jimple.infoflow.threading.IExecutorFactory;

public class PriorityExecutorFactory implements IExecutorFactory {
    @Override
    public InterruptableExecutor createExecutor(int numThreads, boolean allowSetSemantics, InfoflowConfiguration config) {
        if (allowSetSemantics) {
            return new SetPoolExecutor(
                    config.getMaxThreadNum() == -1 ? numThreads : Math.min(config.getMaxThreadNum(), numThreads),
                    Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new PriorityBlockingQueue<>());
        } else {
            return new InterruptableExecutor(
                    config.getMaxThreadNum() == -1 ? numThreads : Math.min(config.getMaxThreadNum(), numThreads),
                    Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new PriorityBlockingQueue<>());
        }
    }
}
