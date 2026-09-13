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
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.ArrayList;
import java.util.List;

/** log4j reads the stack to name a logger, and a page has no stack, so the build names it */
public final class SendLoggerCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String LOG_MANAGER = "org.apache.logging.log4j.LogManager";
    private static final ValueType LOGGER = ValueType.object("org.apache.logging.log4j.Logger");
    private static final ValueType FACTORY =
            ValueType.object("org.apache.logging.log4j.message.MessageFactory");
    private static final ValueType STRING = ValueType.object("java.lang.String");

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        if (type.getName().equals(LOG_MANAGER)) {
            return;
        }
        for (MethodHolder method : type.getMethods()) {
            Program program = method.getProgram();
            if (program == null) {
                continue;
            }
            for (BasicBlock block : program.getBasicBlocks()) {
                List<InvokeInstruction> asks = new ArrayList<>();
                for (Instruction instruction : block) {
                    if (instruction instanceof InvokeInstruction invoke && asksByStack(invoke)) {
                        asks.add(invoke);
                    }
                }
                for (InvokeInstruction invoke : asks) {
                    name(program, invoke, type.getName());
                }
            }
        }
    }

    private static boolean asksByStack(InvokeInstruction invoke) {
        MethodReference asked = invoke.getMethod();
        if (!LOG_MANAGER.equals(asked.getClassName()) || !"getLogger".equals(asked.getName())) {
            return false;
        }
        return asked.parameterCount() == 0
                || asked.parameterCount() == 1 && FACTORY.equals(asked.parameterType(0));
    }

    private static void name(Program program, InvokeInstruction invoke, String caller) {
        Variable name = program.createVariable();
        StringConstantInstruction constant = new StringConstantInstruction();
        constant.setReceiver(name);
        constant.setConstant(caller);
        invoke.insertPrevious(constant);

        List<Variable> arguments = new ArrayList<>();
        arguments.add(name);
        arguments.addAll(invoke.getArguments());
        ValueType[] signature = new ValueType[arguments.size() + 1];
        signature[0] = STRING;
        if (arguments.size() == 2) {
            signature[1] = FACTORY;
        }
        signature[signature.length - 1] = LOGGER;
        invoke.setMethod(new MethodReference(LOG_MANAGER, "getLogger", signature));
        invoke.setArguments(arguments.toArray(new Variable[0]));
    }
}
