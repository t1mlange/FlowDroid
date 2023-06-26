package soot.jimple.infoflow.collections.strategies.widening;

import java.util.Set;

import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;

/**
 * Widens at each shift operation
 *
 * @author Tim Lange
 */
public class WideningOnShiftOperationStrategy extends AbstractWidening {

    // Contains all subsignatures that may result in an infinite domain
    private final Set<String> shiftSigs;

    protected WideningOnShiftOperationStrategy(InfoflowManager manager, Set<String> shiftSigs) {
        super(manager);
        this.shiftSigs = shiftSigs;
    }

    @Override
    public void recordNewFact(Abstraction fact, Unit unit) {
        // NO-OP
    }

    @Override
    public Abstraction widen(Abstraction abs, Unit unit) {
        // Only context in the domain are infinite
        if (abs.getAccessPath().getFragmentCount() == 0 || !abs.getAccessPath().getFirstFragment().hasContext())
            return abs;

        Stmt stmt = (Stmt) unit;
        // Only shifting can produce infinite ascending chains
        if (!stmt.containsInvokeExpr()
                || !shiftSigs.contains(stmt.getInvokeExpr().getMethod().getSubSignature()))
            return abs;

        return forceWiden(abs, unit);
    }
}
