package soot.jimple.infoflow.methodSummary.data.summary;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
	private final boolean preventPropagation;

	public MethodClear(String methodSig, FlowClear clearDefinition, List<FlowConstraint> constraints, boolean preventPropagation) {
		super(methodSig);
		this.clearDefinition = clearDefinition;
		this.constraints = constraints == null || constraints.isEmpty() ? null : constraints;
		this.preventPropagation = preventPropagation;
	}

	public boolean preventPropagation() {
		return preventPropagation;
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
		return new MethodClear(methodSig, clearDefinition.replaceGaps(replacementMap), constraints, preventPropagation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), clearDefinition, constraints, preventPropagation);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		MethodClear that = (MethodClear) o;
		return preventPropagation == that.preventPropagation && Objects.equals(clearDefinition, that.clearDefinition) && Objects.equals(constraints, that.constraints);
	}
}
