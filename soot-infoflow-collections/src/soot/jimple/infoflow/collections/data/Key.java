package soot.jimple.infoflow.collections.data;

public class Key extends Location {
    public Key(int paramIdx) {
        super(paramIdx);
    }

    @Override
    public boolean isValueBased() {
        return false;
    }
}
