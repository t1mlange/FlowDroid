package soot.jimple.infoflow.sourcesSinks.definitions;

import soot.Local;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.AccessPath;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MergedStatementSourceSinkDefinition extends StatementSourceSinkDefinition
        implements IMergedSourceSinkDefinition {
    private Collection<StatementSourceSinkDefinition> originalSourceSinkDefinitions;

    private MergedStatementSourceSinkDefinition(Stmt stmt, Local local, Set<AccessPathTuple> accessPaths, Set<SourceSinkCondition> conditions) {
        super(stmt, local, accessPaths);
        this.originalSourceSinkDefinitions = new HashSet<>();
        this.conditions = conditions;
        this.category = MergedCategory.INSTANCE;
    }

    public static ISourceSinkDefinition create(StatementSourceSinkDefinition stmtDef) {
        HashSet<AccessPathTuple> accessPaths = stmtDef.accessPaths == null ? null : new HashSet<>(stmtDef.accessPaths);
        HashSet<SourceSinkCondition> conditions = stmtDef.conditions == null ? null : new HashSet<>(stmtDef.conditions);
        MergedStatementSourceSinkDefinition merged = new MergedStatementSourceSinkDefinition(stmtDef.stmt, stmtDef.local,
                accessPaths, conditions);
        merged.originalSourceSinkDefinitions.add(stmtDef);
        return merged;
    }

    @Override
    public ISourceSinkDefinition merge(ISourceSinkDefinition other) {
        if (!(other instanceof StatementSourceSinkDefinition))
            throw new IllegalArgumentException("Can not merge StatementSourceSinkDefinition with a definition of another type");

        StatementSourceSinkDefinition otherStmt = (StatementSourceSinkDefinition) other;
        if (this.stmt != otherStmt.stmt || this.local != otherStmt.local)
            throw new IllegalArgumentException("Definitions to be merged do not match!");

        // Merge the base object definitions
        if (otherStmt.accessPaths != null && !otherStmt.accessPaths.isEmpty()) {
            if (this.accessPaths == null)
                this.accessPaths = new HashSet<>();
            this.accessPaths.addAll(otherStmt.accessPaths);
        }

        if (otherStmt.conditions != null) {
            if (this.conditions == null)
                this.conditions = new HashSet<>();
            this.conditions.addAll(otherStmt.conditions);
        }

        originalSourceSinkDefinitions.add(otherStmt);
        return this;
    }

    @Override
    protected MergedStatementSourceSinkDefinition buildNewDefinition(Set<AccessPathTuple> accessPaths) {
        MergedStatementSourceSinkDefinition merged = new MergedStatementSourceSinkDefinition(stmt, local, accessPaths, new HashSet<>());

        for (StatementSourceSinkDefinition origDef : originalSourceSinkDefinitions) {
            if (accessPaths != null && origDef.accessPaths != null
                    && !Collections.disjoint(accessPaths, origDef.accessPaths)) {
                merged.originalSourceSinkDefinitions.add(origDef);
                merged.conditions.addAll(origDef.conditions);
            }
        }

        return merged;
    }

    @Override
    public Collection<? extends ISourceSinkDefinition> getMatchingDefinitions(Stmt stmt, AccessPath ap) {
        HashSet<StatementSourceSinkDefinition> matchingDefinitions = new HashSet<>();
        for (StatementSourceSinkDefinition origDef : originalSourceSinkDefinitions) {
            if (local == ap.getPlainValue()
                    && origDef.accessPaths.stream().anyMatch(apt -> AccessPath.accessPathMatches(ap, apt)))
                matchingDefinitions.add(origDef);
        }
        return matchingDefinitions;
    }
}
