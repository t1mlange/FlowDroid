/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.sourcesSinks.manager;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import heros.solver.IDESolver;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.util.SystemClassHandler;

import java.util.*;

/**
 * A {@link ISourceSinkManager} implementation for backwards analysis
 * This takes the sources and sinks and internally swaps those
 * as the backwards search treats sinks as sources and vice versa.
 *
 * @author Tim Lange
 */
public class BackwardsSourceSinkManager implements ISourceSinkManager {

	protected Collection<String> sourceDefs;
	protected Collection<String> sinkDefs;

	private DefaultSourceSinkManager defSourcesSinks;

	private Collection<SootMethod> sources;
	private Collection<SootMethod> sinks;

	protected final LoadingCache<SootClass, Collection<SootClass>> interfacesOf = IDESolver.DEFAULT_CACHE_BUILDER
			.build(new CacheLoader<SootClass, Collection<SootClass>>() {

				@Override
				public Collection<SootClass> load(SootClass sc) throws Exception {
					Set<SootClass> set = new HashSet<>(sc.getInterfaceCount());
					for (SootClass i : sc.getInterfaces()) {
						set.add(i);
						set.addAll(interfacesOf.getUnchecked(i));
					}
					SootClass superClass = sc.getSuperclassUnsafe();
					if (superClass != null)
						set.addAll(interfacesOf.getUnchecked(superClass));
					return set;
				}

			});

	/**
	 * Creates a new instance of the {@link BackwardsSourceSinkManager} class
	 *
	 * @param sources The list of methods to be treated as sources
	 * @param sinks   The list of methods to be treated as sins
	 */
	public BackwardsSourceSinkManager(Collection<String> sources, Collection<String> sinks) {
		this.defSourcesSinks = new DefaultSourceSinkManager(sources, sinks);
		this.sourceDefs = sources;
		this.sinkDefs = sinks;
	}

	/**
	 * Creates a new instance of the {@link BackwardsSourceSinkManager} class
	 *
	 * @param sourceSinkProvider The provider that defines source and sink methods
	 */
	public BackwardsSourceSinkManager(ISourceSinkDefinitionProvider sourceSinkProvider) {
		this.defSourcesSinks = new DefaultSourceSinkManager(sourceSinkProvider);
		this.sourceDefs = new HashSet<>();
		this.sinkDefs = new HashSet<>();

		// Load the sources
		for (ISourceSinkDefinition ssd : sourceSinkProvider.getSources()) {
			if (ssd instanceof MethodSourceSinkDefinition) {
				MethodSourceSinkDefinition mssd = (MethodSourceSinkDefinition) ssd;
				sourceDefs.add(mssd.getMethod().getSignature());
			}
		}

		// Load the sinks
		for (ISourceSinkDefinition ssd : sourceSinkProvider.getSinks()) {
			if (ssd instanceof MethodSourceSinkDefinition) {
				MethodSourceSinkDefinition mssd = (MethodSourceSinkDefinition) ssd;
				sinkDefs.add(mssd.getMethod().getSignature());
			}
		}
	}

	/**
	 * InterproceduralConstantValuePropagator doesn't like the swapping
	 *
	 * @return DefaultSourceSinkManager instance
	 */
	public DefaultSourceSinkManager getDefaultSourceSinkManager() {
		return defSourcesSinks;
	}

	/**
	 * Sets the list of methods to be treated as sources
	 *
	 * @param sources The list of methods to be treated as sources
	 */
	public void setSources(List<String> sources) {
		defSourcesSinks.setSources(sources);
		this.sourceDefs = sources;
	}

	/**
	 * Sets the list of methods to be treated as sinks
	 *
	 * @param sinks The list of methods to be treated as sinks
	 */
	public void setSinks(List<String> sinks) {
		defSourcesSinks.setSinks(sinks);
		this.sinkDefs = sinks;
	}

	/**
	 * Gets the corresponding method out of a set of (maybe abstract or interface) methods
	 * @param manager manager object giving us access to the iCFG
	 * @param callStmt statement, which could be a source/sink
	 * @param set set to find method in
	 * @return method in set else null
	 */
	private SootMethod getMethodInSet(InfoflowManager manager, Stmt callStmt, Collection<SootMethod> set) {
		// Only method calls can be sources/sinks
		if (!callStmt.containsInvokeExpr() || set == null)
			return null;

		// Method directly matches
		SootMethod callee = callStmt.getInvokeExpr().getMethod();
		if (set.contains(callee))
			return callee;

		// Interface methods
		String subSig = callee.getSubSignature();
		for (SootClass i : interfacesOf.getUnchecked(callee.getDeclaringClass())) {
			SootMethod sm = i.getMethodUnsafe(subSig);
			if (sm != null && set.contains(sm))
				return sm;
		}

		// Try to find method in iCFG
		for (SootMethod sm : manager.getICFG().getCalleesOfCallAt(callStmt)) {
			if (set.contains(sm))
				return sm;
		}

		// nothing ofund
		return null;
	}


	/**
	 * Checks whether the given call sites invokes a source method
	 *
	 * @param manager   The manager object providing access to the configuration and
	 *                  the interprocedural control flow graph
	 * @param sCallSite The call site to check
	 * @return True if the given call site invoked a source method, otherwise false
	 */
	protected boolean isSourceMethod(InfoflowManager manager, Stmt sCallSite) {
		return getMethodInSet(manager, sCallSite, this.sinks) != null;
	}

	/**
	 * Checks whether the given call sites invokes a sink method
	 *
	 * @param manager   The manager object providing access to the configuration and
	 *                  the interprocedural control flow graph
	 * @param sCallSite The call site to check
	 * @return The method that was discovered as a sink, or null if no sink could be
	 *         found
	 */
	protected SootMethodAndClass isSinkMethod(InfoflowManager manager, Stmt sCallSite) {
		SootMethod sm = getMethodInSet(manager, sCallSite, sources);
		return sm == null ? null : new SootMethodAndClass(sm);
	}

	/**
	 * Determines if a method called by the Stmt is a sink and therefor
	 * a source in backwards analysis. If so, additional information is returned
	 *
	 * @param sCallSite
	 *            a Stmt which should include an invokeExrp calling a method
	 * @param manager
	 *            The manager object for interacting with the solver
	 * @return A SourceInfo object containing additional information if this
	 *         call is a source in backwards analysis, otherwise null
	 */
	@Override
	public SourceInfo getSourceInfo(Stmt sCallSite, InfoflowManager manager) {
		SootMethod callee = sCallSite.containsInvokeExpr() ? sCallSite.getInvokeExpr().getMethod() : null;

		Set<AccessPath> aps = new HashSet<>();

		if (isSourceMethod(manager, sCallSite)) {
			InvokeExpr ie = sCallSite.getInvokeExpr();

			// Add the parameter access paths
			for (Value arg : ie.getArgs()) {
				aps.add(manager.getAccessPathFactory().createAccessPath(arg, true));
			}

			// TODO: for easier debugging I decided to comment this out for now. Uncomment later on!
			// Add the base object access path
//			if (ie instanceof InstanceInvokeExpr) {
//				Value base = ((InstanceInvokeExpr) sCallSite.getInvokeExpr()).getBase();
//				aps.add(manager.getAccessPathFactory().createAccessPath(base, true));
//			}
		}

		if (aps.isEmpty())
			return null;

		// Removes possible null ap's
		aps.remove(null);

		// Create the source information data structure
		return new SourceInfo(callee == null ? null : new MethodSourceSinkDefinition(new SootMethodAndClass(callee)),
				aps);
	}

	/**
	 * Determines if a method called by the Stmt is a source and therefor
	 * a sink in backwards analysis. If so, additional information is returned
	 *
	 * @param sCallSite
	 *            a Stmt which should include an invokeExrp calling a method
	 * @param manager
	 *            The manager object for interacting with the solver
	 * @return A SinkInfo object containing additional information if this
	 *         call is a sink in backwards analysis, otherwise null
	 */
	@Override
	public SinkInfo getSinkInfo(Stmt sCallSite, InfoflowManager manager, AccessPath ap) {
		SootMethodAndClass smac = isSinkMethod(manager, sCallSite);
		if (smac != null) {
			InvokeExpr ie = sCallSite.getInvokeExpr();

			if (!SystemClassHandler.v().isTaintVisible(ap, ie.getMethod()))
				return null;

			// Overapproximation if we have no access path
			if (ap == null)
				return new SinkInfo(new MethodSourceSinkDefinition(smac));

			if (!ap.isStaticFieldRef()) {
				// Check if taint is an argument
				for (Value arg : ie.getArgs()) {
					if (arg == ap.getPlainValue()) {
						if (ap.getTaintSubFields() || ap.isLocal())
							return new SinkInfo(new MethodSourceSinkDefinition(smac));
					}
				}

				// Check if base is tainted
				if (ie instanceof InstanceInvokeExpr) {
					if (((InstanceInvokeExpr) ie).getBase() == ap.getPlainValue())
						return new SinkInfo(new MethodSourceSinkDefinition(smac));
				}

				// x = o.m(a1, ..., an)
				// The return value came out of a source (in backwards -> sink)
				// and the left side is tainted
				if (sCallSite instanceof AssignStmt) {
					if(((AssignStmt) sCallSite).getLeftOp() == ap.getPlainValue())
						return new SinkInfo(new MethodSourceSinkDefinition(smac));
				}
			}
		}

		return null;
	}


	/**
	 * Initializes class fields
	 * Call before get(Source|Sink)Info or is(Source|Sink)
	 */
	public void initialize() {
		this.defSourcesSinks.initialize();

		if (sourceDefs != null) {
			sources = new HashSet<>();
			initSootMethodSet(sourceDefs, sources);
			sourceDefs = null;
		}

		if (sinkDefs != null) {
			sinks = new HashSet<>();
			initSootMethodSet(sinkDefs, sinks);
			sinkDefs = null;
		}

	}

	/**
	 * Converts the signatures to SootMethod objects
	 * @param sigSet set of method signature strings
	 * @param smSet set in which the soot methods should be added
	 */
	private void initSootMethodSet(Collection<String> sigSet, Collection<SootMethod> smSet) {
		for (String sig : sigSet) {
			SootMethod sm = Scene.v().grabMethod(sig);
			if (sm != null)
				smSet.add(sm);
		}
	}
}
