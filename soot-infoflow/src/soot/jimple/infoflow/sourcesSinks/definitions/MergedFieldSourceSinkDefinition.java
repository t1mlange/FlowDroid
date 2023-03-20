package soot.jimple.infoflow.sourcesSinks.definitions;

import soot.Local;
import soot.Value;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AccessPath;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MergedFieldSourceSinkDefinition extends FieldSourceSinkDefinition
        implements IMergedSourceSinkDefinition {
    private final Collection<FieldSourceSinkDefinition> originalSourceSinkDefinitions;

    private MergedFieldSourceSinkDefinition(String fieldSignature, Set<AccessPathTuple> accessPaths,
                                           Set<SourceSinkCondition> conditions) {
        super(fieldSignature, accessPaths);
        this.originalSourceSinkDefinitions = new HashSet<>();
        this.conditions = conditions;
        this.category = MergedCategory.INSTANCE;
    }

    public static MergedFieldSourceSinkDefinition create(FieldSourceSinkDefinition def) {
        HashSet<SourceSinkCondition> conditions = def.conditions == null ? null : new HashSet<>(def.conditions);
        HashSet<AccessPathTuple> accessPaths = def.accessPaths == null ? null : new HashSet<>(def.accessPaths);
        MergedFieldSourceSinkDefinition merged = new MergedFieldSourceSinkDefinition(def.fieldSignature,
                accessPaths, conditions);
        merged.originalSourceSinkDefinitions.add(def);
        return merged;
    }

    @Override
    public ISourceSinkDefinition merge(ISourceSinkDefinition other) {
        if (!(other instanceof FieldSourceSinkDefinition))
            throw new IllegalArgumentException("Can not merge source sink definitions of different types!");

        FieldSourceSinkDefinition otherField = (FieldSourceSinkDefinition) other;

        // Merge the base object definitions
        if (otherField.accessPaths != null && !otherField.accessPaths.isEmpty()) {
            if (this.accessPaths == null)
                this.accessPaths = new HashSet<>();
            this.accessPaths.addAll(otherField.accessPaths);
        }

        if (otherField.conditions != null) {
            if (this.conditions == null)
                this.conditions = new HashSet<>();
            this.conditions.addAll(otherField.conditions);
        }

        originalSourceSinkDefinitions.add(otherField);
        return this;
    }

    @Override
    protected FieldSourceSinkDefinition buildNewDefinition(Set<AccessPathTuple> accessPaths) {
        MergedFieldSourceSinkDefinition merged = new MergedFieldSourceSinkDefinition(fieldSignature, accessPaths, new HashSet<>());

        for (FieldSourceSinkDefinition origDef : originalSourceSinkDefinitions) {
            if (accessPaths != null && origDef.accessPaths != null
                    && !Collections.disjoint(accessPaths, origDef.accessPaths)) {
                merged.originalSourceSinkDefinitions.add(origDef);
                merged.conditions.addAll(origDef.conditions);
            }
        }

        return merged.originalSourceSinkDefinitions.size() == 0 ? null : merged;
    }

    @Override
    public Collection<? extends ISourceSinkDefinition> getMatchingDefinitions(Stmt stmt, AccessPath ap) {
        if (!(stmt instanceof AssignStmt))
            return Collections.emptySet();

        AssignStmt assign = (AssignStmt) stmt;

        HashSet<FieldSourceSinkDefinition> matchingDefinitions = new HashSet<>();

        for (FieldSourceSinkDefinition origDef : originalSourceSinkDefinitions) {
            boolean retain = false;
            for (AccessPathTuple apt : origDef.accessPaths) {
                if (apt.getSourceSinkType() == SourceSinkType.Source
                        || apt.getSourceSinkType() == SourceSinkType.Both) {
                    if (assign.getLeftOp() == ap.getPlainValue() && AccessPath.accessPathMatches(ap, apt)) {
                        retain = true;
                        break;
                    }
                }

                if (apt.getSourceSinkType() == SourceSinkType.Sink
                        || apt.getSourceSinkType() == SourceSinkType.Both) {
                    if (assign.getRightOp() == ap.getPlainValue() && AccessPath.accessPathMatches(ap, apt)) {
                        retain = true;
                        break;
                    }
                }
            }
            if (retain)
                matchingDefinitions.add(origDef);
        }

        return matchingDefinitions;
    }
}
