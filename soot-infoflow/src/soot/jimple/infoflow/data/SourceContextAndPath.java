package soot.jimple.infoflow.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import heros.solver.Pair;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.PathConfiguration;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.util.extensiblelist.ExtensibleList;

/**
 * Extension of {@link SourceContext} that also allows a paths from the source
 * to the current statement to be stored
 * 
 * @author Steven Arzt
 */
public class SourceContextAndPath extends SourceContext implements Cloneable {

	protected ExtensibleList<Abstraction> path = null;
	protected ExtensibleList<Stmt> callStack = null;
	protected int neighborCounter = 0;
	protected InfoflowConfiguration config;

	private int hashCode = 0;

	public SourceContextAndPath(InfoflowConfiguration config, ISourceSinkDefinition definition, AccessPath value,
			Stmt stmt) {
		this(config, definition, value, stmt, null);
	}

	public SourceContextAndPath(InfoflowConfiguration config, ISourceSinkDefinition definition, AccessPath value,
			Stmt stmt, Object userData) {
		super(definition, value, stmt, userData);
		this.config = config;
	}

	public List<Stmt> getPath() {
		if (path == null)
			return Collections.<Stmt>emptyList();
		List<Stmt> stmtPath = new ArrayList<>(this.path.size());
		Iterator<Abstraction> it = path.reverseIterator();
		while (it.hasNext()) {
			Abstraction abs = it.next();
			if (abs.getCurrentStmt() != null) {
				stmtPath.add(abs.getCurrentStmt());
			}
		}
		return stmtPath;
	}

	public List<Abstraction> getAbstractionPath() {
		if (path == null)
			return null;

		List<Abstraction> reversePath = new ArrayList<>(path.size());
		Iterator<Abstraction> it = path.reverseIterator();
		while (it.hasNext()) {
			reversePath.add(it.next());
		}
		return reversePath;
	}

	public List<Stmt> getCallStack() {
		if (callStack == null)
			return null;

		List<Stmt> reversePath = new ArrayList<>(callStack.size());
		Iterator<Stmt> it = callStack.reverseIterator();
		while (it.hasNext()) {
			reversePath.add(it.next());
		}
		return reversePath;
	}

	public Abstraction getLastAbstraction() {
		return path.getLast();
	}

	public SourceContextAndPath extendPath(SourceContextAndPath other) {
		SourceContextAndPath newScap = clone();

		if (other != null) {
			for (Abstraction abs : other.getAbstractionPath())
				newScap.path.add(abs);

			List<Stmt> stmts = other.getCallStack();
			for (Stmt stmt : stmts)
				newScap.callStack.add(stmt);

			Abstraction abs = other.path.getLast();
			this.neighborCounter = abs.getNeighbors() == null ? 0 : abs.getNeighbors().size();

		}

		return newScap;
	}

	/**
	 * Extends the taint propagation path with the given abstraction
	 * 
	 * @param abs The abstraction to put on the taint propagation path
	 * @return The new taint propagation path If this path would contain a loop,
	 *         null is returned instead of the looping path.
	 */
	public SourceContextAndPath extendPath(Abstraction abs) {
		return extendPath(abs, null);
	}

	/**
	 * Extends the taint propagation path with the given abstraction
	 * 
	 * @param abs    The abstraction to put on the taint propagation path
	 * @param config The configuration for constructing taint propagation paths
	 * @return The new taint propagation path. If this path would contain a loop,
	 *         null is returned instead of the looping path.
	 */
	public SourceContextAndPath extendPath(Abstraction abs, InfoflowConfiguration config) {
		if (abs == null)
			return this;

		// If we have no data at all, there is nothing we can do here
		if (abs.getCurrentStmt() == null && abs.getCorrespondingCallSite() == null)
			return this;

		final PathConfiguration pathConfig = config == null ? null : config.getPathConfiguration();

		// If we don't track paths and have nothing to put on the stack, there
		// is no need to create a new object
		final boolean trackPath = pathConfig == null || pathConfig.getPathReconstructionMode().reconstructPaths();
		if (abs.getCorrespondingCallSite() == null && !trackPath)
			return this;

		// Do not add the very same abstraction over and over again.
		if (this.path != null) {
			Iterator<Abstraction> it = path.reverseIterator();
			while (it.hasNext()) {
				Abstraction a = it.next();
				if (a == abs)
					return null;
			}
		}

		SourceContextAndPath scap = null;
		if (trackPath && abs.getCurrentStmt() != null) {
			if (this.path != null) {
				// We cannot leave the same method at two different sites
				Abstraction topAbs = path.getLast();
				if (topAbs.equals(abs) && topAbs.getCorrespondingCallSite() != null
						&& topAbs.getCorrespondingCallSite() == abs.getCorrespondingCallSite()
						&& topAbs.getCurrentStmt() != abs.getCurrentStmt())
					return null;
			}

			scap = clone();

			// Extend the propagation path
			if (scap.path == null)
				scap.path = new ExtensibleList<Abstraction>();
			scap.path.add(abs);

			if (pathConfig != null && pathConfig.getMaxPathLength() > 0
					&& scap.path.size() > pathConfig.getMaxPathLength()) {
				return null;
			}
		}

		// Extend the call stack
		switch (config.getDataFlowDirection()) {
		case Forwards:
			if (abs.getCorrespondingCallSite() != null && abs.getCorrespondingCallSite() != abs.getCurrentStmt()) {
				if (scap == null)
					scap = this.clone();
				if (scap.callStack == null)
					scap.callStack = new ExtensibleList<Stmt>();
				else if (pathConfig != null && pathConfig.getMaxCallStackSize() > 0
						&& scap.callStack.size() >= pathConfig.getMaxCallStackSize())
					return null;
				scap.callStack.add(abs.getCorrespondingCallSite());
			}
			break;
		case Backwards:
			if (abs.getCurrentStmt() != null && abs.getCurrentStmt().containsInvokeExpr()
					&& abs.getCorrespondingCallSite() != abs.getCurrentStmt()) {
				if (scap == null)
					scap = this.clone();
				if (scap.callStack == null)
					scap.callStack = new ExtensibleList<Stmt>();
				else if (pathConfig != null && pathConfig.getMaxCallStackSize() > 0
						&& scap.callStack.size() >= pathConfig.getMaxCallStackSize())
					return null;
				scap.callStack.add(abs.getCurrentStmt());
			}
			break;
		}

		this.neighborCounter = abs.getNeighbors() == null ? 0 : abs.getNeighbors().size();
		return scap == null ? this : scap;
	}

	/**
	 * Pops the top item off the call stack.
	 * 
	 * @return The new {@link SourceContextAndPath} object as the first element of
	 *         the pair and the call stack item that was popped off as the second
	 *         element. If there is no call stack, null is returned.
	 */
	public Pair<SourceContextAndPath, Stmt> popTopCallStackItem() {
		if (callStack == null || callStack.isEmpty())
			return null;

		SourceContextAndPath scap = clone();
		Stmt lastStmt = null;
		Object c = scap.callStack.removeLast();
		if (c instanceof ExtensibleList) {
			lastStmt = scap.callStack.getLast();
			scap.callStack = (ExtensibleList<Stmt>) c;
		} else
			lastStmt = (Stmt) c;

		if (scap.callStack.isEmpty())
			scap.callStack = null;
		return new Pair<>(scap, lastStmt);
	}

	public SourceContextAndPath pushToCallStack(Stmt stmt) {
		final PathConfiguration pathConfig = config == null ? null : config.getPathConfiguration();

		SourceContextAndPath scap = clone();
		if (scap.callStack == null)
			scap.callStack = new ExtensibleList<Stmt>();
		else if (pathConfig != null && pathConfig.getMaxCallStackSize() > 0
				&& scap.callStack.size() >= pathConfig.getMaxCallStackSize())
			return null;
		scap.callStack.add(stmt);

		return scap;
	}

	/**
	 * Gets whether the current call stack is empty, i.e., the path is in the method
	 * from which it originated
	 * 
	 * @return True if the call stack is empty, otherwise false
	 */
	public boolean isCallStackEmpty() {
		return this.callStack == null || this.callStack.isEmpty();
	}

	public void setNeighborCounter(int counter) {
		this.neighborCounter = counter;
	}

	public int getNeighborCounter() {
		return this.neighborCounter;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other == null || !(other instanceof SourceContextAndPath))
			return false;
		SourceContextAndPath scap = (SourceContextAndPath) other;

		if (this.hashCode != 0 && scap.hashCode != 0 && this.hashCode != scap.hashCode)
			return false;

		boolean mergeDifferentPaths = !config.getPathAgnosticResults() && path != null && scap.path != null;
		if (mergeDifferentPaths) {
			if (path.size() != scap.path.size()) {
				// Quick check: they cannot be equal
				return false;
			}
		}

		if (this.callStack == null || this.callStack.isEmpty()) {
			if (scap.callStack != null && !scap.callStack.isEmpty())
				return false;
		} else {
			if (scap.callStack == null || scap.callStack.isEmpty())
				return false;

			if (callStack.size() != scap.callStack.size() || !this.callStack.equals(scap.callStack))
				return false;
		}

		if (mergeDifferentPaths) {
			if (!this.path.equals(scap.path))
				return false;
		}

		return super.equals(other);
	}

	@Override
	public int hashCode() {
		if (hashCode != 0)
			return hashCode;

		final int prime = 31;
		int result = super.hashCode();
		if (!config.getPathAgnosticResults())
			result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((callStack == null) ? 0 : callStack.hashCode());
		this.hashCode = result;
		return hashCode;
	}

	@Override
	public SourceContextAndPath clone() {
		final SourceContextAndPath scap = new SourceContextAndPath(config, definition, accessPath, stmt, userData);
		if (path != null)
			scap.path = new ExtensibleList<Abstraction>(this.path);
		if (callStack != null)
			scap.callStack = new ExtensibleList<Stmt>(callStack);
		return scap;
	}

	@Override
	public String toString() {
		return super.toString() + "\n\ton Path: " + getAbstractionPath();
	}
}
