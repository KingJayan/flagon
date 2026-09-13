package dev.flagon.shims;

import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class Reroute {

    private Reroute() {
    }

    static void apply(ClassHolder type, String subject, String target,
                      Map<String, String> methods, String[] notOurs) {
        if (isOurs(type.getName(), notOurs)) {
            return;
        }
        for (MethodHolder method : type.getMethods()) {
            Program program = method.getProgram();
            if (program == null) {
                continue;
            }
            for (BasicBlock block : program.getBasicBlocks()) {
                for (Instruction instruction : block) {
                    if (instruction instanceof InvokeInstruction invoke) {
                        send(invoke, subject, target, methods);
                    }
                }
            }
        }
    }

    private static void send(InvokeInstruction invoke, String subject, String target,
                             Map<String, String> methods) {
        MethodReference asked = invoke.getMethod();
        if (!subject.equals(asked.getClassName())) {
            return;
        }
        String replacement = methods.get(asked.getName() + ":" + asked.parameterCount());
        if (replacement == null) {
            replacement = methods.get(asked.getName());
        }
        if (replacement == null) {
            return;
        }
        Variable receiver = invoke.getInstance();
        List<Variable> arguments = new ArrayList<>(invoke.getArguments());
        List<ValueType> signature = new ArrayList<>();
        if (receiver != null) {
            arguments.add(0, receiver);
            signature.add(ValueType.object(subject));
        }
        for (int at = 0; at < asked.parameterCount(); at++) {
            signature.add(asked.parameterType(at));
        }
        signature.add(asked.getReturnType());

        invoke.setInstance(null);
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(new MethodReference(target, replacement,
                signature.toArray(new ValueType[0])));
        invoke.setArguments(arguments.toArray(new Variable[0]));
    }

    private static boolean isOurs(String name, String[] notOurs) {
        for (String prefix : notOurs) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
