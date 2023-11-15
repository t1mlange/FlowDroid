package soot.jimple.infoflow.android.callbacks.codeql;

import org.xmlpull.v1.XmlPullParserException;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.android.manifest.IManifestHandler;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.toolkits.callgraph.ReachableMethods;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

/**
 * Handles unsound callback resolving of Soot and adds edges to every call site with a callback but no callback callee.
 * Should put us closer to the approximations taken by CodeQL when analysis Android applications.
 */
public class CallbackEnricher implements PreAnalysisHandler {
    private String pkgName;
    public CallbackEnricher(String apk) {
        try {
            pkgName = new ProcessManifest(apk).getPackageName();
        } catch (Exception e) {
            pkgName = "";
        }
    }

    @Override
    public void onBeforeCallgraphConstruction() {

    }

    private List<? extends Value> makeDummyArgs(SootMethod sm) {
        List<Type> types = sm.getParameterTypes();
        List<Value> args = new ArrayList<>(types.size());
        for (Type t : types) {
            TypeSwitch<Value> tsw = new TypeSwitch<Value>() {
                @Override
                public void caseBooleanType(BooleanType t) {
                    setResult(IntConstant.v(0));
                }

                @Override
                public void caseByteType(ByteType t) {
                    setResult(IntConstant.v(0));
                }

                @Override
                public void caseCharType(CharType t) {
                    setResult(IntConstant.v(0));
                }

                @Override
                public void caseDoubleType(DoubleType t) {
                    setResult(DoubleConstant.v(0));
                }

                @Override
                public void caseFloatType(FloatType t) {
                    setResult(FloatConstant.v(0));
                }

                @Override
                public void caseIntType(IntType t) {
                    setResult(IntConstant.v(0));
                }

                @Override
                public void caseLongType(LongType t) {
                    setResult(LongConstant.v(0));
                }

                @Override
                public void caseShortType(ShortType t) {
                    setResult(IntConstant.v(0));
                }

                @Override
                public void defaultCase(Type t) {
                    setResult(NullConstant.v());
                }
            };
            t.apply(tsw);
            args.add(tsw.getResult());
        }
        return args;
    }

    private int counter = 0;
    private String getNextName() {
        return "myUniqL" + counter++;
    }

    @Override
    public void onAfterCallgraphConstruction() {
        ReachableMethods rm = Scene.v().getReachableMethods();
        rm.update();

        File f = new File("unique_packages.txt");
        if (!f.exists())
            throw new RuntimeException("WHERE IS UNIQUE_PACKAGES");
        List<String> lines;
        try {
            lines = Files.readAllLines(f.getAbsoluteFile().toPath(), Charset.defaultCharset());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Set<SootMethod> mset = new HashSet<>();
        for (SootClass sc : Scene.v().getClasses()) {
            if (lines.stream().noneMatch(line -> sc.getName().startsWith(line))) {
                sc.setLibraryClass();
                continue;
            }

            for (SootMethod sm : sc.getMethods()) {
                if (rm.contains(sm) || sm.isStaticInitializer() || sm.isPhantom() || sm.isAbstract() || !sm.isPublic())
                    continue;

                sm.retrieveActiveBody();
                if (!sm.hasActiveBody())
                    continue;

                mset.add(sm);
            }
        }

        SootClass mainClass = Scene.v().getSootClass("dummyMainClass");
        SootMethod myCaller = mainClass.getMethodUnsafe("void makeAppClassesReachable()");
        if (myCaller == null) {
            myCaller = Scene.v().makeSootMethod("makeAppClassesReachable", null, VoidType.v());
            myCaller.setModifiers(myCaller.getModifiers() | Modifier.STATIC);
            myCaller.setDeclaringClass(Scene.v().getSootClass("dummyMainClass"));
            mainClass.addMethod(myCaller);
        }

        Body b = Jimple.v().newBody(myCaller);
        b.getUnits().add(Jimple.v().newNopStmt());
        for (SootMethod sm : mset) {
            if (sm.isStatic() && sm.isConcrete()) {
                b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(sm.makeRef(), makeDummyArgs(sm))));
            } else if (sm.isConstructor() && sm.isConcrete()) {
                Local l = Jimple.v().newLocal(getNextName(), sm.getDeclaringClass().getType());
                b.getUnits().add(Jimple.v().newAssignStmt(l, Jimple.v().newNewExpr(sm.getDeclaringClass().getType())));
                b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(l, sm.makeRef(), makeDummyArgs(sm))));
            } else if (sm.isConcrete()) {
                Local l = Jimple.v().newLocal(getNextName(), sm.getDeclaringClass().getType());
                b.getUnits().add(Jimple.v().newAssignStmt(l, Jimple.v().newNewExpr(sm.getDeclaringClass().getType())));
                SootMethod c = sm.getDeclaringClass().getMethods().stream().filter(m -> m.getName().equals("<init>")).min(Comparator.comparingInt(SootMethod::getParameterCount)).get();
                b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(l, c.makeRef(), makeDummyArgs(c))));
                b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(l, sm.makeRef(), makeDummyArgs(sm))));
            }
        }
        myCaller.setActiveBody(b);
        SootMethod ep = Scene.v().grabMethod("<dummyMainClass: void dummyMainMethod(java.lang.String[])>");
        ep.getActiveBody().getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(myCaller.makeRef())));
    }
}
