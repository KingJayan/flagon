package dev.flagon.shims;

import org.junit.jupiter.api.Test;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.StringConstantInstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class SendLoggerCallsTest {

    private static final String LOG_MANAGER = "org.apache.logging.log4j.LogManager";
    private static final String CALLER = "org.example.Game";
    private static final ValueType LOGGER = ValueType.object("org.apache.logging.log4j.Logger");
    private static final ValueType FACTORY =
            ValueType.object("org.apache.logging.log4j.message.MessageFactory");
    private static final ValueType STRING = ValueType.object("java.lang.String");

    @Test
    void namesTheLoggerAfterTheCaller() {
        InvokeInstruction invoke = transform(LOGGER);

        assertEquals(new MethodReference(LOG_MANAGER, "getLogger", STRING, LOGGER),
                invoke.getMethod());
        assertNamedAfterCaller(invoke);
    }

    @Test
    void namesTheLoggerAfterTheCallerWhenAFactoryIsGiven() {
        InvokeInstruction invoke = transform(FACTORY, LOGGER);

        assertEquals(new MethodReference(LOG_MANAGER, "getLogger", STRING, FACTORY, LOGGER),
                invoke.getMethod());
        assertNamedAfterCaller(invoke);
    }

    @Test
    void keepsALoggerThatAlreadyHasAName() {
        InvokeInstruction invoke = transform(STRING, LOGGER);

        assertEquals(new MethodReference(LOG_MANAGER, "getLogger", STRING, LOGGER),
                invoke.getMethod());
        assertFalse(invoke.getPrevious() instanceof StringConstantInstruction);
    }

    private static void assertNamedAfterCaller(InvokeInstruction invoke) {
        StringConstantInstruction name =
                assertInstanceOf(StringConstantInstruction.class, invoke.getPrevious());
        assertEquals(CALLER, name.getConstant());
        assertSame(name.getReceiver(), invoke.getArguments().get(0));
    }

    private static InvokeInstruction transform(ValueType... signature) {
        Program program = new Program();
        program.createVariable();
        BasicBlock block = program.createBasicBlock();

        Variable[] arguments = new Variable[signature.length - 1];
        for (int at = 0; at < arguments.length; at++) {
            arguments[at] = program.createVariable();
        }
        InvokeInstruction invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(new MethodReference(LOG_MANAGER, "getLogger", signature));
        invoke.setArguments(arguments);
        invoke.setReceiver(program.createVariable());
        block.add(invoke);

        MethodHolder method = new MethodHolder(MethodDescriptor.parse("start()V"));
        method.setProgram(program);
        ClassHolder type = new ClassHolder(CALLER);
        type.addMethod(method);

        new SendLoggerCalls().transformClass(type, null);
        return invoke;
    }
}
