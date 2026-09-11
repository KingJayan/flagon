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

import java.util.Map;

/**
 * sends a one-argument call on a classlib type to a static Flagon method that takes the
 * receiver first. the shims all have this shape, so they all share this.
 */
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
        if (!subject.equals(asked.getClassName()) || invoke.getInstance() == null) {
            return;
        }
        String replacement = methods.get(asked.getName());
        if (replacement == null || asked.parameterCount() != 1) {
            return;
        }
        Variable receiver = invoke.getInstance();
        Variable argument = invoke.getArguments().get(0);
        invoke.setInstance(null);
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(new MethodReference(target, replacement,
                ValueType.object(subject), asked.parameterType(0), asked.getReturnType()));
        invoke.setArguments(receiver, argument);
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
