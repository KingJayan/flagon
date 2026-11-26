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
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

public final class SendUuidCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String SUBJECT = "java.util.UUID";
    private static final String UUIDS = "dev.flagon.platform.Uuids";

    private static final Map<String, String> SENT = Map.of(
            "getMostSignificantBits", "getMostSignificantBits",
            "getLeastSignificantBits", "getLeastSignificantBits",
            "nameUUIDFromBytes", "nameUUIDFromBytes",
            "fromString", "fromString");

    private static final String[] NOT_OURS = {"org.teavm.", UUIDS};

    private static final MethodReference OF = new MethodReference(UUIDS, "of",
            ValueType.LONG, ValueType.LONG, ValueType.object(SUBJECT));

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, SUBJECT, UUIDS, SENT, NOT_OURS);
        if (type.getName().startsWith("org.teavm.") || UUIDS.equals(type.getName())) {
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
                        build(invoke);
                    }
                }
            }
        }
    }

    private static void build(InvokeInstruction invoke) {
        MethodReference asked = invoke.getMethod();
        if (!SUBJECT.equals(asked.getClassName()) || !"<init>".equals(asked.getName())
                || asked.parameterCount() != 2 || asked.parameterType(0) != ValueType.LONG) {
            return;
        }
        invoke.setReceiver(invoke.getInstance());
        invoke.setInstance(null);
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(OF);
    }
}
