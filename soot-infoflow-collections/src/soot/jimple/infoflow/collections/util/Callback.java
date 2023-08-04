package soot.jimple.infoflow.collections.util;

import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;

public class Callback {
    Abstraction d1;
    Abstraction d2;
    Unit u;

    public Callback(Abstraction d1, Unit u, Abstraction d2) {
        this.d1 = d1;
        this.u = u;
        this.d2 = d2;
    }

    public Unit getUnit() {
        return u;
    }

    public Abstraction getD1() {
        return d1;
    }

    public Abstraction getD2() {
        return d2;
    }
}