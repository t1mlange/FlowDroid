package soot.jimple.infoflow.collections.util;

public class Tristate {
    private enum State {
        TRUE,
        FALSE,
        MAYBE
    }

    private final State internal;
    private Tristate(State s) {
        this.internal = s;
    }

    private static final Tristate TRUE_INSTANCE = new Tristate(State.TRUE);
    private static final Tristate FALSE_INSTANCE = new Tristate(State.FALSE);
    private static final Tristate MAYBE_INSTANCE = new Tristate(State.MAYBE);

    public static Tristate TRUE() {
        return TRUE_INSTANCE;
    }

    public static Tristate FALSE() {
        return FALSE_INSTANCE;
    }

    public static Tristate MAYBE() {
        return MAYBE_INSTANCE;
    }

    public boolean isTrue() {
        return internal == State.TRUE;
    }

    public boolean isFalse() {
        return internal == State.FALSE;
    }

    public boolean isMaybe() {
        return internal == State.MAYBE;
    }

    public Tristate and(Tristate other) {
        if (this.isTrue() && other.isTrue())
            return TRUE();

        if (this.isFalse() || other.isFalse())
            return FALSE();

        return MAYBE();
    }
}
