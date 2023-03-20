package soot.jimple.infoflow.sourcesSinks.definitions;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.AccessPath;

import java.util.Collection;

/**
 * Special source sink definition that merges multiple source sink conditions of the same statement
 * into one for the analysis.
 */
public interface IMergedSourceSinkDefinition extends ISourceSinkDefinition {
    /**
     * Returns the original SourceSinkDefinitions that match the statement and access path
     *
     * @param stmt statement
     * @param ap access path
     * @return all matching SourceSinkDefinitions
     */
    Collection<? extends ISourceSinkDefinition> getMatchingDefinitions(Stmt stmt, AccessPath ap);
}
