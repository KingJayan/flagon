package dev.flagon.shims;

import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

public final class SendShutdownHooks implements TeaVMPlugin, ClassHolderTransformer {

    private static final String SUBJECT = "java.lang.Runtime";
    private static final String HOOKS = "dev.flagon.platform.ShutdownHooks";

    private static final Map<String, String> SENT = Map.of(
            "addShutdownHook", "add",
            "removeShutdownHook", "remove",
            "exit", "exit",
            "halt", "halt");

    private static final String[] NOT_OURS = {"org.teavm.", HOOKS};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        if (isOurs(type.getName())) {
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
                        send(invoke);
                    }
                }
            }
        }
    }

    private static void send(InvokeInstruction invoke) {
        MethodReference asked = invoke.getMethod();
        if (!SUBJECT.equals(asked.getClassName()) || invoke.getInstance() == null) {
            return;
        }
        String replacement = SENT.get(asked.getName());
        if (replacement == null || asked.parameterCount() != 1) {
            return;
        }
        Variable runtime = invoke.getInstance();
        Variable argument = invoke.getArguments().get(0);
        invoke.setInstance(null);
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(new MethodReference(HOOKS, replacement,
                ValueType.object(SUBJECT), asked.parameterType(0), asked.getReturnType()));
        invoke.setArguments(runtime, argument);
    }

    private static boolean isOurs(String name) {
        for (String prefix : NOT_OURS) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
