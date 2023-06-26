package soot.jimple.infoflow.collections.solver.fastSolver;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;

import soot.jimple.infoflow.collections.strategies.subsuming.SubsumingStrategy;
import soot.toolkits.graph.DirectedGraph;

public class ContainerDependenceAnalysis {
    public static <N, D> boolean analyze(DirectedGraph<N> ug, SubsumingStrategy<N, D> subsuming) {
        HashSet<N> visited = new HashSet<>();

        Deque<N> worklist = new ArrayDeque<>(ug.getHeads());
        while (!worklist.isEmpty()) {
            N current = worklist.remove();

            if (subsuming.affectsContext(current))
                return true;

            for (N succ : ug.getSuccsOf(current))
                if (visited.add(succ))
                    worklist.add(succ);
        }

        return false;
    }
}
