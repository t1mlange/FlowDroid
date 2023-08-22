package soot.jimple.infoflow.collections.data;

import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowConstraint;
import soot.jimple.infoflow.methodSummary.data.summary.ImplicitLocation;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.taintWrappers.AccessPathFragment;
import soot.jimple.infoflow.util.BaseSelector;

public class IndexConstraint extends FlowConstraint {
    private final ImplicitLocation loc;
    private final boolean updateMode;

    public IndexConstraint(SourceSinkType type, int paramIdx, String baseType, AccessPathFragment accessPathFragment,
                           ImplicitLocation loc, boolean updateMode) {
        super(type, paramIdx, baseType, accessPathFragment);
        this.loc = loc;
        this.updateMode = updateMode;
    }

    @Override
    public boolean isIndexBased() {
        return true;
    }

    @Override
    public boolean mightShift() {
        return !updateMode;
    }

    @Override
    public ImplicitLocation getImplicitLocation() {
        assert getType() == SourceSinkType.Field;
        return loc;
    }
}
