package soot.jimple.infoflow.collections.solver.fastSolver;

import soot.jimple.Stmt;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Map;
import java.util.Set;

public class MethodCallback {
    static class Callback {
        MyConcurrentHashMap<Stmt, Abstraction> map;
        boolean isDirty;
        final Object lock;

        public Callback() {
            map = new MyConcurrentHashMap<>();
            isDirty = false;
            lock = new Object();
        }

        public boolean add(Stmt stmt, Abstraction abs) {
            synchronized (lock) {
                isDirty = true;
                return map.put(stmt, abs) == null;
            }
        }

        private void cleanupAndGet() {
            synchronized (lock) {

            }
        }
    }

    private Map<Abstraction, Callback> callbacks;


}
