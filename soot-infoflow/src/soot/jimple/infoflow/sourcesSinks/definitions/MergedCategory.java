package soot.jimple.infoflow.sourcesSinks.definitions;

public class MergedCategory implements ISourceSinkCategory {
    protected static MergedCategory INSTANCE = new MergedCategory();

    @Override
    public String getHumanReadableDescription() {
        return "Multiple categories. Unpack the merged categories first!";
    }

    @Override
    public String getID() {
        return "Merged Category";
    }
}
