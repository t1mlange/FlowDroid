package soot.jimple.infoflow.collections.data;

public class Index extends Location {
    public Index(int paramIdx) {
        super(paramIdx);
    }

    @Override
    public boolean isValueBased() {
        return true;
    }
}
