package soot.jimple.infoflow.collections.operations;

import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collections.data.Location;
import soot.jimple.infoflow.collections.data.ParamIndex;
import soot.jimple.infoflow.collections.strategies.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.data.ContextDefinition;
import soot.jimple.infoflow.typing.TypeUtils;

import java.util.Collection;

public class InsertOperation extends LocationDependentOperation {
    private final int data;

    public InsertOperation(Location[] keys, int data, String field, String fieldType) {
        super(keys, field, fieldType);
        this.data = data;
    }

    private SootField safeGetField(String fieldSig) {
        if (fieldSig == null || fieldSig.equals(""))
            return null;

        SootField sf = Scene.v().grabField(fieldSig);
        if (sf != null)
            return sf;

        // This field does not exist, so we need to create it
        String className = fieldSig.substring(1);
        className = className.substring(0, className.indexOf(":"));
        SootClass sc = Scene.v().getSootClassUnsafe(className, true);
        if (sc.resolvingLevel() < SootClass.SIGNATURES && !sc.isPhantom()) {
            System.err.println("WARNING: Class not loaded: " + sc);
            return null;
        }

        String type = fieldSig.substring(fieldSig.indexOf(": ") + 2);
        type = type.substring(0, type.indexOf(" "));

        String fieldName = fieldSig.substring(fieldSig.lastIndexOf(" ") + 1);
        fieldName = fieldName.substring(0, fieldName.length() - 1);

        SootFieldRef ref = Scene.v().makeFieldRef(sc, fieldName, TypeUtils.getTypeFromString(type), false);
        return ref.resolve();
    }

    @Override
    public boolean apply(Abstraction d1, Abstraction incoming, Stmt stmt, InfoflowManager manager, IContainerStrategy strategy, Collection<Abstraction> out) {
        InstanceInvokeExpr iie = ((InstanceInvokeExpr) stmt.getInvokeExpr());
        if (!manager.getAliasing().mayAlias(incoming.getAccessPath().getPlainValue(), iie.getArg(data)))
            return false;

        Value base = iie.getBase();
        ContextDefinition[] ctxt = new ContextDefinition[keys.length];
        for (int i = 0; i < ctxt.length; i++) {
            if (keys[i].getParamIdx() == ParamIndex.LAST_INDEX.toInt())
                ctxt[i] = strategy.getNextPosition(base, stmt);
            else if (keys[i].isValueBased())
                ctxt[i] = strategy.getIndexContext(iie.getArg(keys[i].getParamIdx()), stmt);
            else
                ctxt[i] = strategy.getKeyContext(iie.getArg(keys[i].getParamIdx()), stmt);
        }
        if (strategy.shouldSmash(ctxt))
            ctxt = null;

        AccessPathFragment[] oldFragments = incoming.getAccessPath().getFragments();
        int len = oldFragments == null ? 0 : oldFragments.length;
        AccessPathFragment[] fragments = new AccessPathFragment[len + 1];
        if (oldFragments != null)
            System.arraycopy(oldFragments, 0, fragments, 1, len);
        SootField f = safeGetField(field);
        fragments[0] = new AccessPathFragment(f, f.getType(), ctxt);
        AccessPath ap = manager.getAccessPathFactory().createAccessPath(base, fragments, incoming.getAccessPath().getTaintSubFields());
        out.add(incoming.deriveNewAbstraction(ap, stmt));

        // Insert never removes an element
        return false;
    }
}
