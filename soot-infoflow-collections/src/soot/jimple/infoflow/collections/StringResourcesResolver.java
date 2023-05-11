package soot.jimple.infoflow.collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.codeOptimization.ICodeOptimizer;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.util.queue.QueueReader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

/**
 * Resolves Android String resources to constants in the code
 *
 * @author Tim Lange
 */
public class StringResourcesResolver implements ICodeOptimizer {
    private static final String CONTEXT_CLASS = "android.content.Context";
    private static final String GET_STRING_SUBSIG = "java.lang.String getString(int)";

    private static class ReplacementCandidate {
        SootMethod method;
        Stmt oldStmt;
        Stmt newStmt;
        ReplacementCandidate(SootMethod method, Stmt oldStmt, Stmt newStmt) {
            this.method = method;
            this.oldStmt = oldStmt;
            this.newStmt = newStmt;
        }

        void replace(IInfoflowCFG icfg) {
            method.getActiveBody().getUnits().swapWith(oldStmt, newStmt);
            icfg.notifyMethodChanged(method);
        }
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String fileName = null;
    private int runtime = -1;
    private int replaced = -1;

    public int getRuntime() {
        return runtime;
    }

    public int getReplacedStatementCount() {
        return replaced;
    }

    @Override
    public void initialize(InfoflowConfiguration config) {
        if (config instanceof InfoflowAndroidConfiguration)
            this.fileName = ((InfoflowAndroidConfiguration) config).getAnalysisFileConfig().getTargetAPKFile();
    }

    @Override
    public void run(InfoflowManager manager, Collection<SootMethod> excluded, ISourceSinkManager sourcesSinks, ITaintPropagationWrapper taintWrapper) {
        if (fileName == null)
            return;

        long beforeOptimization = System.nanoTime();

        ARSCFileParser parser;
        try {
            parser = ARSCFileParser.getInstance(new File(fileName));
        } catch (IOException e) {
            logger.error("Could not parse the ARSC file!", e);
            return;
        }
        assert parser != null;

        SootClass contextClass = Scene.v().getSootClassUnsafe(CONTEXT_CLASS);

        // First pass: Collect all statements to be replaced
        HashSet<ReplacementCandidate> toBeReplaced = new HashSet<>();
        for (QueueReader<MethodOrMethodContext> rdr = Scene.v().getReachableMethods().listener(); rdr.hasNext();) {
            MethodOrMethodContext sm = rdr.next();
            SootMethod method = sm.method();
            if (method == null || !method.hasActiveBody() || excluded.contains(method))
                continue;

            UnitPatchingChain chain = method.getActiveBody().getUnits();
            for (Unit unit : chain) {
                Stmt stmt = (Stmt) unit;
                // We only care for calls to two methods, where the result is not ignored
                if (!stmt.containsInvokeExpr() || !(stmt instanceof AssignStmt))
                    continue;

                // Check whether the invoke expressions calls a method we care about
                SootMethod callee = stmt.getInvokeExpr().getMethod();
                String subSig = callee.getSubSignature();
                if (!manager.getHierarchy().isSubclass(callee.getDeclaringClass(), contextClass)
                        || !subSig.equals(GET_STRING_SUBSIG))
                    continue;

                // Extract the resource id
                Value arg0 = stmt.getInvokeExpr().getArg(0);
                if (!(arg0 instanceof IntConstant))
                    continue;
                int resourceId = ((IntConstant) arg0).value;

                // Get the string for the given resource id
                ARSCFileParser.AbstractResource res = parser.findResource(resourceId);
                if (!(res instanceof ARSCFileParser.StringResource))
                    continue;
                String str = ((ARSCFileParser.StringResource) res).getValue();

                // Construct new constant assignment
                AssignStmt constantAssign = Jimple.v().newAssignStmt(((AssignStmt) stmt).getLeftOp(), StringConstant.v(str));
                constantAssign.addTag(SimulatedCodeElementTag.TAG);
                toBeReplaced.add(new ReplacementCandidate(method, stmt, constantAssign));
            }
        }

        // Second pass: replace statements
        for (ReplacementCandidate r : toBeReplaced)
            r.replace(manager.getICFG());

        replaced = toBeReplaced.size();
        runtime = (int) Math.round((System.nanoTime() - beforeOptimization) / 1E9);
        logger.info(String.format("Resolved %d android string resources in %d seconds.", replaced, runtime));
    }
}
