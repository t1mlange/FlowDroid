package soot.jimple.infoflow.collections.solver.fastSolver;

import java.util.Map;

import soot.jimple.Stmt;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.Abstraction;

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
