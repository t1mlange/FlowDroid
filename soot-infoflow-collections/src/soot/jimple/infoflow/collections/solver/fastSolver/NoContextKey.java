package soot.jimple.infoflow.collections.solver.fastSolver;

import soot.SootMethod;
import soot.jimple.infoflow.data.Abstraction;

public class NoContextKey {
    private final SootMethod sm;
    private final Abstraction abs;
    private int hashCode;

    public NoContextKey(SootMethod sm, Abstraction abs) {
        this.sm = sm;
        this.abs = abs;
        this.hashCode = 0;
    }

    @Override
    public int hashCode() {
        if (hashCode != 0)
            return hashCode;

        final int prime = 31;
        int result = 1;
        result = prime * result + ((sm == null) ? 0 : sm.hashCode());
        result = prime * result + ((abs.getSourceContext() == null) ? 0 : abs.getSourceContext().hashCode());
        result = prime * result + ((abs.getAccessPath() == null) ? 0 : abs.getAccessPath().hashCodeWithoutContext());
        result = prime * result + ((abs.getActivationUnit() == null) ? 0 : abs.getActivationUnit().hashCode());
        result = prime * result + ((abs.getTurnUnit() == null) ? 0 : abs.getTurnUnit().hashCode());
        result = prime * result + (abs.getExceptionThrown() ? 1231 : 1237);
        result = prime * result + (abs.dependsOnCutAP() ? 1231 : 1237);
        result = prime * result + (abs.isImplicit() ? 1231 : 1237);
        this.hashCode = result;

        return this.hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        NoContextKey other = (NoContextKey) obj;

        if (this.hashCode != 0 && other.hashCode != 0 && this.hashCode != other.hashCode)
            return false;

        if (sm == null) {
            if (other.sm != null)
                return false;
        } else if (!sm.equals(other.sm))
            return false;

        if (abs.getAccessPath() == null) {
            if (other.abs.getAccessPath() != null)
                return false;
        } else if (!abs.getAccessPath().equalsWithoutContext(other.abs.getAccessPath()))
            return false;

        return abs.localEquals(other.abs);
    }
}
