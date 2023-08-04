package soot.jimple.infoflow.collections.context;

import soot.jimple.infoflow.data.ContextDefinition;

/**
 * Representing an unknown context (e.g. non-constant key) using a singleton
 *
 * @author Tim Lange
 */
public class UnknownContext implements ContextDefinition {
    private static final UnknownContext INSTANCE = new UnknownContext();

    public static UnknownContext v() {
        return INSTANCE;
    }

    private UnknownContext() {}

    @Override
    public String toString() {
        return "<Unknown Context>";
    }

    @Override
    public boolean containsInformation() {
        return false;
    }

    @Override
    public boolean entails(ContextDefinition other) {
        // The unknown context entails all other contexts
        return true;
    }
}
