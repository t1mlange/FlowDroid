package soot.jimple.infoflow.util;

import soot.Unit;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;

import java.util.Set;

public class DebugFlowFunctionTaintPropagationHandler implements TaintPropagationHandler {
    @Override
    public void notifyFlowIn(Unit stmt, Abstraction taint, InfoflowManager manager, FlowFunctionType type) {
        // no-op
    }

    @Override
    public Set<Abstraction> notifyFlowOut(Unit stmt, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing, InfoflowManager manager, FlowFunctionType type) {
        String typeString = "";
        switch (type) {
            case CallToReturnFlowFunction:
                typeString = "CallToReturn";
                break;
            case ReturnFlowFunction:
                typeString = "Return";
                break;
            case CallFlowFunction:
                typeString = "Call";
                break;
            case NormalFlowFunction:
                typeString = "Normal";
                break;
        }

        System.out.println(typeString + " @ " + stmt + ":\n\tIn: " + incoming + "\n\tOut: " + outgoing + "\n");

        return outgoing;
    }
}
