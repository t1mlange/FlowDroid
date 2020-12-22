//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package soot.jimple.infoflow.util;

import heros.InterproceduralCFG;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeStmt;
import soot.util.dot.DotGraph;

public class ICFGRecusiveDotVisualizer {
    private static final Logger logger = LoggerFactory.getLogger(ICFGRecusiveDotVisualizer.class);
    private DotGraph dotIcfg = new DotGraph("");
    private ArrayList<Unit> visited = new ArrayList();
    String fileName;
    Unit startPoint;
    InterproceduralCFG<Unit, SootMethod> icfg;

    public ICFGRecusiveDotVisualizer(String fileName, Unit startPoint, InterproceduralCFG<Unit, SootMethod> icfg) {
        this.fileName = fileName;
        this.startPoint = startPoint;
        this.icfg = icfg;
        if (this.fileName == null || this.fileName == "") {
            System.out.println("Please provide a vaid filename");
        }

        if (this.startPoint == null) {
            System.out.println("startPoint is null!");
        }

        if (this.icfg == null) {
            System.out.println("ICFG is null!");
        }

    }

    public void exportToDot() {
        if (this.startPoint != null && this.icfg != null && this.fileName != null) {
            this.graphTraverse(this.startPoint, this.icfg);
            this.dotIcfg.plot(this.fileName);
            logger.debug("" + this.fileName + ".dot");
        } else {
            System.out.println("Parameters not properly initialized!");
        }

    }

    private void graphTraverse(Unit startPoint, InterproceduralCFG<Unit, SootMethod> icfg) {
        List<Unit> currentSuccessors = icfg.getSuccsOf(startPoint);
        if (startPoint instanceof InvokeStmt) {
            Collection<SootMethod> callees = icfg.getCalleesOfCallAt(startPoint);
            for (SootMethod callee : callees) {
                Collection<Unit> calleeStartPoints = icfg.getStartPointsOf(callee);

                for (Unit calleeStartPoint : calleeStartPoints) {
                    this.dotIcfg.drawEdge(startPoint.toString(), calleeStartPoint.toString());
                    this.graphTraverse(calleeStartPoint, icfg);
                }
            }
        }
        if (currentSuccessors.size() == 0) {
            System.out.println("Traversal complete");
        } else {
            for (Unit succ : currentSuccessors) {
                System.out.println("Succesor: " + succ.toString());
                if (!this.visited.contains(succ)) {
                    this.dotIcfg.drawEdge(startPoint.toString(), succ.toString());
                    this.visited.add(succ);
                    this.graphTraverse(succ, icfg);
                } else {
                    this.dotIcfg.drawEdge(startPoint.toString(), succ.toString());
                }
            }
        }
    }
}
