package soot.jimple.infoflow.problems.rules;

import soot.jimple.ArrayRef;
import soot.jimple.infoflow.data.ContextDefinition;

public interface IArrayPropagationRule {
    /**
     * Returns the context definition for an array
     *
     * @param arrayRef array reference
     * @return context definition
     */
    ContextDefinition[] getContextForArrayRef(ArrayRef arrayRef);
}
