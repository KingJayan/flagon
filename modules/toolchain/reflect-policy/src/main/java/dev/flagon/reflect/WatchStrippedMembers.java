package dev.flagon.reflect;

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

/**
 * sends the application's Class member queries through {@link ReflectionWatch}, so an empty
 * answer is reported rather than passed on in silence.
 */
public final class WatchStrippedMembers implements TeaVMPlugin, ClassHolderTransformer {

    private static final String SUBJECT = "java.lang.Class";
    private static final String WATCH = "dev.flagon.platform.ReflectionWatch";

    private static final Map<String, String> WATCHED = Map.of(
            "getDeclaredMethods", "declaredMethods",
            "getMethods", "methods",
            "getDeclaredFields", "declaredFields",
            "getFields", "fields",
            "getDeclaredConstructors", "declaredConstructors",
            "getConstructors", "constructors");

    /** the runtime reflects over itself for its own reasons; only application code is watched */
    private static final String[] NOT_OURS = {
        "java.", "javax.", "jdk.", "sun.", "org.teavm.", WATCH,
    };

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        if (isRuntime(type.getName())) {
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
                        retarget(invoke);
                    }
                }
            }
        }
    }

    private static void retarget(InvokeInstruction invoke) {
        MethodReference asked = invoke.getMethod();
        if (!SUBJECT.equals(asked.getClassName()) || invoke.getInstance() == null) {
            return;
        }
        String watched = WATCHED.get(asked.getName());
        if (watched == null) {
            return;
        }
        Variable subject = invoke.getInstance();
        invoke.setInstance(null);
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(new MethodReference(WATCH, watched,
                ValueType.object(SUBJECT), asked.getReturnType()));
        invoke.setArguments(subject);
    }

    private static boolean isRuntime(String name) {
        for (String prefix : NOT_OURS) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
