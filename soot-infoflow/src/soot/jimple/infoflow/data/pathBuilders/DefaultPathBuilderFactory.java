package soot.jimple.infoflow.data.pathBuilders;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import soot.jimple.infoflow.InfoflowConfiguration.PathBuildingAlgorithm;
import soot.jimple.infoflow.InfoflowConfiguration.PathConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;

/**
 * Default factory class for abstraction path builders
 * 
 * @author Steven Arzt
 */
public class DefaultPathBuilderFactory implements IPathBuilderFactory {

	private final PathConfiguration pathConfiguration;

	/**
	 * Creates a new instance of the {@link DefaultPathBuilderFactory} class
	 * 
	 * @param config
	 *            The configuration for reconstructing data flow paths
	 */
	public DefaultPathBuilderFactory(PathConfiguration config) {
		this.pathConfiguration = config;
	}

	/**
	 * Creates a new executor object for spawning worker threads
	 * 
	 * @param maxThreadNum
	 *            The number of threads to use
	 * @return The generated executor
	 */
	private InterruptableExecutor createExecutor(int maxThreadNum) {
		int numThreads = Runtime.getRuntime().availableProcessors();
		InterruptableExecutor executor = new InterruptableExecutor(
				maxThreadNum == -1 ? numThreads : Math.min(maxThreadNum, numThreads), Integer.MAX_VALUE, 30,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		executor.setThreadFactory(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread thr = new Thread(r, "Path reconstruction");
				return thr;
			}
		});
		return executor;
	}

	@Override
	public IAbstractionPathBuilder createPathBuilder(InfoflowManager manager, int maxThreadNum, InfoflowResults results) {
		return createPathBuilder(manager, createExecutor(maxThreadNum), results);
	}

	@Override
	public IAbstractionPathBuilder createPathBuilder(InfoflowManager manager, InterruptableExecutor executor, InfoflowResults results) {
		switch (pathConfiguration.getPathBuildingAlgorithm()) {
		case Recursive:
			return new RecursivePathBuilder(manager, executor);
		case ContextSensitive:
			return new ContextSensitivePathBuilder(manager, executor, results);
		case ContextInsensitive:
			return new ContextInsensitivePathBuilder(manager, executor, results);
		case ContextInsensitiveSourceFinder:
			return new ContextInsensitiveSourceFinder(manager, executor, results);
		case None:
			return new EmptyPathBuilder();
		}
		throw new RuntimeException("Unsupported path building algorithm");
	}

	@Override
	public boolean supportsPathReconstruction() {
		switch (pathConfiguration.getPathBuildingAlgorithm()) {
		case Recursive:
		case ContextSensitive:
		case ContextInsensitive:
			return true;
		case ContextInsensitiveSourceFinder:
		case None:
			return false;
		}
		throw new RuntimeException("Unsupported path building algorithm");
	}

	@Override
	public boolean isContextSensitive() {
		return pathConfiguration.getPathBuildingAlgorithm() == PathBuildingAlgorithm.ContextSensitive;
	}

}
