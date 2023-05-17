package soot.jimple.infoflow.collections.context;

import soot.jimple.infoflow.data.ContextDefinition;

public class IntervalContext implements ContextDefinition {
    private int min;
    private int max;

    public IntervalContext(int i) {
        min = i;
        max = i;
    }


}