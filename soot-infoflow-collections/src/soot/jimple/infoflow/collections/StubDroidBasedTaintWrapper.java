package soot.jimple.infoflow.collections;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.sourceSink.AbstractFlowSinkSource;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.taintWrappers.AccessPathPropagator;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.jimple.infoflow.methodSummary.taintWrappers.Taint;
import soot.jimple.infoflow.typing.TypeUtils;

public class StubDroidBasedTaintWrapper extends SummaryTaintWrapper {
    /**
     * Creates a new instance of the {@link SummaryTaintWrapper} class
     *
     * @param flows The flows loaded from disk
     */
    public StubDroidBasedTaintWrapper(IMethodSummaryProvider flows) {
        super(flows);
    }

    protected AccessPathPropagator applyFlow(MethodFlow flow, AccessPathPropagator propagator) {
        final AbstractFlowSinkSource flowSource = flow.source();
        AbstractFlowSinkSource flowSink = flow.sink();
        final Taint taint = propagator.getTaint();

        // Make sure that the base type of the incoming taint and the one of
        // the summary are compatible
        boolean typesCompatible = flowSource.getBaseType() == null
                || isCastCompatible(TypeUtils.getTypeFromString(taint.getBaseType()),
                TypeUtils.getTypeFromString(flowSource.getBaseType()));
        if (!typesCompatible)
            return null;

        // If this flow starts at a gap, our current taint must be at that gap
        if (taint.getGap() != flow.source().getGap())
            return null;

        // Maintain the stack of access path propagations
        final AccessPathPropagator parent;
        final GapDefinition gap, taintGap;
        final Stmt stmt;
        final Abstraction d1, d2;
        if (flowSink.getGap() != null) { // ends in gap, push on stack
            parent = propagator;
            gap = flowSink.getGap();
            stmt = null;
            d1 = null;
            d2 = null;
            taintGap = null;
        } else {
            parent = safePopParent(propagator);
            gap = propagator.getParent() == null ? null : propagator.getParent().getGap();
            stmt = propagator.getParent() == null ? propagator.getStmt() : propagator.getParent().getStmt();
            d1 = propagator.getParent() == null ? propagator.getD1() : propagator.getParent().getD1();
            d2 = propagator.getParent() == null ? propagator.getD2() : propagator.getParent().getD2();
            taintGap = propagator.getGap();
        }

        boolean addTaint = flowMatchesTaint(flowSource, taint);

        // If we didn't find a match, there's little we can do
        if (!addTaint)
            return null;

        // Construct a new propagator
        Taint newTaint = null;
        if (flow.isCustom()) {
            newTaint = addCustomSinkTaint(flow, taint, taintGap);
        } else
            newTaint = addSinkTaint(flow, taint, taintGap);
        if (newTaint == null)
            return null;

        AccessPathPropagator newPropagator = new AccessPathPropagator(newTaint, gap, parent, stmt, d1, d2,
                propagator.isInversePropagator());
        return newPropagator;
    }

    protected boolean flowMatchesTaint(final AbstractFlowSinkSource flowSource, final Taint taint) {
        if (flowSource.isParameter() && taint.isParameter()) {
            // Get the parameter index from the call and compare it to the
            // parameter index in the flow summary
            if (taint.getParameterIndex() == flowSource.getParameterIndex()) {
                if (compareFields(taint, flowSource))
                    return true;
            }
        } else if (flowSource.isField()) {
            // Flows from a field can either be applied to the same field or
            // the base object in total
            boolean doTaint = (taint.isGapBaseObject() || taint.isField());
            if (doTaint && compareFields(taint, flowSource))
                return true;
        }
        // We can have a flow from a local or a field
        else if (flowSource.isThis() && taint.isField())
            return true;
            // A value can also flow from the return value of a gap to somewhere
        else if (flowSource.isReturn() && flowSource.getGap() != null && taint.getGap() != null
                && compareFields(taint, flowSource))
            return true;
            // For aliases, we over-approximate flows from the return edge to all
            // possible exit nodes
        else if (flowSource.isReturn() && flowSource.getGap() == null && taint.getGap() == null && taint.isReturn()
                && compareFields(taint, flowSource))
            return true;
        return false;
    }

    protected boolean compareFields(Taint taintedPath, AbstractFlowSinkSource flowSource) {
        // if we have x.f....fn and the source is x.f'.f1'...f'n+1 and we don't
        // taint sub, we can't have a match
        if (taintedPath.getAccessPathLength() < flowSource.getAccessPathLength()) {
            if (!taintedPath.taintSubFields() || flowSource.isMatchStrict())
                return false;
        }

        // Compare the shared sub-path
        for (int i = 0; i < taintedPath.getAccessPathLength() && i < flowSource.getAccessPathLength(); i++) {
            String taintField = taintedPath.getAccessPath().getField(i);
            String sourceField = flowSource.getAccessPath().getField(i);
            if (!sourceField.equals(taintField))
                return false;

            if (flowSource.isConstrained()) {
                flowSource.getAccessPath().getContext(0);
            }
        }

        return true;
    }
}
