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
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.ArrayList;
import java.util.List;

public final class SendThreadCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String THREAD = "java.lang.Thread";
    private static final ValueType GROUP = ValueType.object("java.lang.ThreadGroup");

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        if (type.getName().startsWith("org.teavm.")) {
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
                        if (asksForGroup(invoke)) {
                            NullConstantInstruction none = new NullConstantInstruction();
                            none.setReceiver(invoke.getReceiver());
                            invoke.replace(none);
                        } else {
                            drop(invoke);
                        }
                    }
                }
            }
        }
    }

    // a page has one thread group, and the constructors that take one drop it
    private static boolean asksForGroup(InvokeInstruction invoke) {
        MethodReference asked = invoke.getMethod();
        return "getThreadGroup".equals(asked.getName()) && asked.parameterCount() == 0
                && invoke.getReceiver() != null
                && (THREAD.equals(asked.getClassName())
                        || "java.lang.SecurityManager".equals(asked.getClassName()));
    }

    private static void drop(InvokeInstruction invoke) {
        MethodReference asked = invoke.getMethod();
        if (!THREAD.equals(asked.getClassName()) || !"<init>".equals(asked.getName())
                || asked.parameterCount() == 0 || !GROUP.equals(asked.parameterType(0))) {
            return;
        }
        List<ValueType> kept = new ArrayList<>();
        List<Variable> arguments = new ArrayList<>();
        for (int at = 1; at < asked.parameterCount(); at++) {
            if (asked.parameterType(at) == ValueType.LONG) {
                continue;
            }
            kept.add(asked.parameterType(at));
            arguments.add(invoke.getArguments().get(at));
        }
        kept.add(ValueType.VOID);
        invoke.setMethod(new MethodReference(THREAD, "<init>", kept.toArray(new ValueType[0])));
        invoke.setArguments(arguments.toArray(new Variable[0]));
    }
}
