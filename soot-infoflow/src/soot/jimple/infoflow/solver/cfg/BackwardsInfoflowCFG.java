package soot.jimple.infoflow.solver.cfg;

import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.IfStmt;
import soot.jimple.SwitchStmt;
import soot.jimple.toolkits.ide.icfg.BackwardsInterproceduralCFG;
import soot.toolkits.graph.DirectedGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inverse interprocedural control-flow graph for the infoflow solver
 * 
 * @author Steven Arzt
 * @author Eric Bodden
 */
public class BackwardsInfoflowCFG extends InfoflowCFG {

	private final IInfoflowCFG baseCFG;

	public BackwardsInfoflowCFG(IInfoflowCFG baseCFG) {
		super(new BackwardsInterproceduralCFG(baseCFG));
		this.baseCFG = baseCFG;
	}

	public IInfoflowCFG getBaseCFG() {
		return this.baseCFG;
	}

	@Override
	public boolean isStaticFieldRead(SootMethod method, SootField variable) {
		return baseCFG.isStaticFieldRead(method, variable);
	}

	@Override
	public boolean isStaticFieldUsed(SootMethod method, SootField variable) {
		return baseCFG.isStaticFieldUsed(method, variable);
	}

	@Override
	public boolean hasSideEffects(SootMethod method) {
		return baseCFG.hasSideEffects(method);
	}

	@Override
	public boolean methodReadsValue(SootMethod m, Value v) {
		return baseCFG.methodReadsValue(m, v);
	}

	@Override
	public boolean methodWritesValue(SootMethod m, Value v) {
		return baseCFG.methodWritesValue(m, v);
	}

	@Override
	public UnitContainer getPostdominatorOf(Unit u) {
		return baseCFG.getPostdominatorOf(u);
	}

	@Override
	public UnitContainer getDominatorOf(Unit u) {
		return baseCFG.getDominatorOf(u);
	}

	@Override
	public boolean isExceptionalEdgeBetween(Unit u1, Unit u2) {
		return super.isExceptionalEdgeBetween(u2, u1);
	}

	@Override
	public Unit getConditionalBranch(Unit unit) {
	DirectedGraph<Unit> graph = getOrCreateUnitGraph(getMethodOf(unit));

		List<Unit> worklist = new ArrayList<>(sameLevelPredecessors(graph, unit));
		while (worklist.size() > 0) {
			Unit item = worklist.remove(0);
			if (item instanceof IfStmt || item instanceof SwitchStmt)
				return item;

			worklist.addAll(sameLevelPredecessors(graph, item));
		}
		return null;
	}

	private List<Unit> sameLevelPredecessors(DirectedGraph<Unit> graph, Unit u) {
		List<Unit> preds = graph.getPredsOf(u);
		if (preds.size() <= 1)
			return preds;

		UnitContainer dom = getDominatorOf(u);
		if (dom.getUnit() != null)
			return graph.getPredsOf(dom.getUnit());
		return Collections.emptyList();
	}

	@Override
	public void notifyMethodChanged(SootMethod m) {
		baseCFG.notifyMethodChanged(m);
	}

	@Override
	public void purge() {
		baseCFG.purge();
		super.purge();
	}

}
