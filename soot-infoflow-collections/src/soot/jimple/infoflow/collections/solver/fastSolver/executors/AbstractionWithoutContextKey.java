package soot.jimple.infoflow.collections.solver.fastSolver.executors;

import soot.jimple.infoflow.data.Abstraction;

public class AbstractionWithoutContextKey {
    private final Abstraction abs;
    private int hashCode;

    public AbstractionWithoutContextKey(Abstraction abs) {
        this.abs = abs;
        this.hashCode = 0;
    }

    @Override
    public int hashCode() {
        if (hashCode != 0)
            return hashCode;

        final int prime = 31;
        int result = 1;
        result = prime * result + ((abs.getSourceContext() == null) ? 0 : abs.getSourceContext().hashCode());
//        result = prime * result + ((accessPath == null) ? 0 : accessPath.hashCode());
        result = prime * result + ((abs.getActivationUnit() == null) ? 0 : abs.getActivationUnit().hashCode());
        result = prime * result + ((abs.getTurnUnit() == null) ? 0 : abs.getTurnUnit().hashCode());
        result = prime * result + (abs.getExceptionThrown() ? 1231 : 1237);
        result = prime * result + (abs.dependsOnCutAP() ? 1231 : 1237);
        result = prime * result + (abs.isImplicit() ? 1231 : 1237);
        this.hashCode = result;

        return super.hashCode();
    }

//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj)
//            return true;
//        if (obj == null || getClass() != obj.getClass())
//            return false;
//        AbstractionWithoutContextKey other = (AbstractionWithoutContextKey) obj;
//
//        if (this.hashCode != 0 && other.hashCode != 0 && this.hashCode != other.hashCode)
//            return false;
//
//        if (!abs.localEquals(other.abs))
//            return false;
//
//        return
//    }
}
