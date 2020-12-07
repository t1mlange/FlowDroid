package soot.jimple.infoflow.problems.rules;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;

import java.util.ArrayList;
import java.util.List;

/**
 * Backward implementation of the {@link IPropagationRuleManagerFactory} class
 * 
 * @author Tim Lange
 *
 */
public class BackwardPropagationRuleManagerFactory implements IPropagationRuleManagerFactory {

	@Override
	public PropagationRuleManager createRuleManager(InfoflowManager manager, Abstraction zeroValue,
													TaintPropagationResults results) {
		List<ITaintPropagationRule> ruleList = new ArrayList<>();

		ruleList.add(new BackwardSinkPropagationRule(manager, zeroValue, results));
		ruleList.add(new BackwardSourcePropagationRule(manager, zeroValue, results));
		ruleList.add(new SkipSystemClassRule(manager, zeroValue, results));

		if (manager.getConfig().getEnableExceptionTracking())
			ruleList.add(new BackwardExceptionPropagationRule(manager, zeroValue, results));
		if (manager.getConfig().getStopAfterFirstKFlows() > 0)
			ruleList.add(new StopAfterFirstKFlowsPropagationRule(manager, zeroValue, results));
		if (manager.getConfig().getEnableArrayTracking())
			ruleList.add(new BackwardArrayPropagationRule(manager, zeroValue, results));


//		ruleList.add(new StaticPropagationRule(manager, zeroValue, results));
//		if (manager.getTaintWrapper() != null)
//			ruleList.add(new WrapperPropagationRule(manager, zeroValue, results));
//		if (manager.getConfig().getImplicitFlowMode().trackControlFlowDependencies())
//			ruleList.add(new ImplicitPropagtionRule(manager, zeroValue, results));
//		ruleList.add(new StrongUpdatePropagationRule(manager, zeroValue, results));
//		if (manager.getConfig().getEnableTypeChecking())
//			ruleList.add(new BackwardTypingPropagationRule(manager, zeroValue, results));

		return new PropagationRuleManager(manager, zeroValue, results,
				ruleList.toArray(new ITaintPropagationRule[0]));
	}

}
