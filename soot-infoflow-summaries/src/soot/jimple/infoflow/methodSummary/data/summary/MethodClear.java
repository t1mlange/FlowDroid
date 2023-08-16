package soot.jimple.infoflow.methodSummary.data.summary;

import java.util.List;
import java.util.Map;

import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowClear;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowConstraint;

/**
 * A taint clearing definition. This class models the fact that a library method
 * clears an existing taint.
 * 
 * @author Steven Arzt
 *
 */
public class MethodClear extends AbstractMethodSummary {

	private final FlowClear clearDefinition;
	private final List<FlowConstraint> constraints;

	public MethodClear(String methodSig, FlowClear clearDefinition, List<FlowConstraint> constraints) {
		super(methodSig);
		this.clearDefinition = clearDefinition;
		this.constraints = constraints == null || constraints.isEmpty() ? null : constraints;
	}

	/**
	 * Gets the definition of the taint that shall be cleared
	 * 
	 * @return The definition of the taint that shall be cleared
	 */
	public FlowClear getClearDefinition() {
		return clearDefinition;
	}

	@Override
	public MethodClear replaceGaps(Map<Integer, GapDefinition> replacementMap) {
		if (replacementMap == null)
			return this;
		return new MethodClear(methodSig, clearDefinition.replaceGaps(replacementMap), constraints);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((clearDefinition == null) ? 0 : clearDefinition.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MethodClear other = (MethodClear) obj;
		if (clearDefinition == null) {
			if (other.clearDefinition != null)
				return false;
		} else if (!clearDefinition.equals(other.clearDefinition))
			return false;
		return true;
	}

}
