package soot.jimple.infoflow.sourcesSinks.definitions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.SootMethodAndClass;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MergedMethodSourceSinkDefinition extends MethodSourceSinkDefinition
        implements IMergedSourceSinkDefinition {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private Collection<MethodSourceSinkDefinition> originalSourceSinkDefinitions;

    private MergedMethodSourceSinkDefinition(SootMethodAndClass smac, Set<AccessPathTuple> baseObjects,
                                            Set<AccessPathTuple>[] parameters, Set<AccessPathTuple> returnValues,
                                            CallType callType, Set<SourceSinkCondition> conditions) {
        super(smac, baseObjects, parameters, returnValues, callType);
        this.originalSourceSinkDefinitions = new HashSet<>();
        this.conditions = conditions;
        this.category = MergedCategory.INSTANCE;
    }

    public static MergedMethodSourceSinkDefinition create(MethodSourceSinkDefinition mssd) {
        Set<AccessPathTuple> baseObjects = mssd.baseObjects == null ? null : new HashSet<>(mssd.baseObjects);
        Set<AccessPathTuple>[] parameters = new Set[mssd.parameters.length];
        for (int i = 0; i < mssd.parameters.length; i++)
            parameters[i] = new HashSet<>(mssd.parameters[i]);
        Set<AccessPathTuple> returnValues = mssd.returnValues == null ? null : new HashSet<>(mssd.returnValues);
        HashSet<SourceSinkCondition> conditions = mssd.conditions == null ? null : new HashSet<>(mssd.conditions);

        MergedMethodSourceSinkDefinition merged = new MergedMethodSourceSinkDefinition(
                mssd.method, baseObjects, parameters, returnValues, mssd.callType, conditions);
        merged.originalSourceSinkDefinitions.add(mssd);
        return merged;
    }

    @Override
    public ISourceSinkDefinition merge(ISourceSinkDefinition def) {
        if (!(def instanceof MethodSourceSinkDefinition))
            throw new IllegalArgumentException("Can not merge source sink definitions of different types!");

        MethodSourceSinkDefinition mssd = (MethodSourceSinkDefinition) def;
        if (mssd.callType != this.callType)
            logger.warn("Merging two different call types, information loss is expected...");

        if (mssd.baseObjects != null && !mssd.baseObjects.isEmpty()) {
            if (this.baseObjects == null)
                this.baseObjects = new HashSet<>();
            this.baseObjects.addAll(mssd.baseObjects);
        }

        // Merge the parameter definitions
        if (mssd.parameters != null && mssd.parameters.length > 0) {
            if (this.parameters == null)
                this.parameters = new Set[this.method.getParameters().size()];
            for (int i = 0; i < mssd.parameters.length; i++) {
                addParameterDefinition(i, mssd.parameters[i]);
            }
        }

        // Merge the return value definitions
        if (mssd.returnValues != null && !mssd.returnValues.isEmpty()) {
            if (this.returnValues == null)
                this.returnValues = new HashSet<>();
            this.returnValues.addAll(mssd.returnValues);
        }

        if (mssd.conditions != null) {
            if (this.conditions == null)
                this.conditions = new HashSet<>();
            this.conditions.addAll(mssd.conditions);
        }

        this.originalSourceSinkDefinitions.add(mssd);
        return this;
    }

    @Override
    protected MergedMethodSourceSinkDefinition buildNewDefinition(Set<AccessPathTuple> baseAPTs,
                                                            Set<AccessPathTuple>[] paramAPTs, Set<AccessPathTuple> returnAPTs) {
        MergedMethodSourceSinkDefinition merged = new MergedMethodSourceSinkDefinition(method, baseAPTs, paramAPTs,
                                                                                    returnAPTs, callType, new HashSet<>());

        for (MethodSourceSinkDefinition origDef : originalSourceSinkDefinitions) {
            boolean retain =
                    (baseAPTs != null && origDef.baseObjects != null && !Collections.disjoint(baseAPTs, origDef.baseObjects))
                    || (returnAPTs != null && origDef.returnValues != null && !Collections.disjoint(returnAPTs, origDef.returnValues));

            if (!retain) {
                int n = Math.min(paramAPTs.length, origDef.parameters.length);
                for (int i = 0; i < n; i++) {
                    if (!Collections.disjoint(paramAPTs[i], origDef.parameters[i])) {
                        retain = true;
                        break;
                    }
                }
            }

            if (retain) {
                merged.conditions.addAll(origDef.conditions);
                merged.originalSourceSinkDefinitions.add(origDef);
            }
        }

        return merged.originalSourceSinkDefinitions.size() == 0 ? null : merged;
    }

    @Override
    public Collection<? extends ISourceSinkDefinition> getMatchingDefinitions(Stmt stmt, AccessPath ap) {
        if (!stmt.containsInvokeExpr())
            return Collections.emptySet();
        InvokeExpr iexpr = stmt.getInvokeExpr();

        Set<MethodSourceSinkDefinition> matchingDefinitions = new HashSet<>();
        for (MethodSourceSinkDefinition origDef : originalSourceSinkDefinitions) {
            if (iexpr instanceof InstanceInvokeExpr
                    && ((InstanceInvokeExpr) iexpr).getBase() == ap.getPlainValue()
                    && origDef.baseObjects.stream().anyMatch(apt -> AccessPath.accessPathMatches(ap, apt))) {
                matchingDefinitions.add(origDef);
                continue;

            }

            if (stmt instanceof AssignStmt
                    && ((AssignStmt) stmt).getLeftOp() == ap.getPlainValue()
                    && origDef.returnValues.stream().anyMatch(apt -> AccessPath.accessPathMatches(ap, apt))) {
                matchingDefinitions.add(origDef);
                continue;
            }

            for (int i = 0; i < origDef.parameters.length; i++) {
                if (origDef.parameters[i] == null)
                    continue;
                if (i >= iexpr.getArgCount())
                    break;

                Value arg = iexpr.getArg(i);
                if (arg == ap.getPlainValue()
                        && origDef.parameters[i].stream().anyMatch(apt -> AccessPath.accessPathMatches(ap, apt))) {
                    matchingDefinitions.add(origDef);
                    break;
                }
            }
        }

        return matchingDefinitions;
    }
}
