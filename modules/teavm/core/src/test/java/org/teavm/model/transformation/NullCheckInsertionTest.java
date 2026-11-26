package org.teavm.model.transformation;

import org.junit.jupiter.api.Test;
import org.teavm.model.BasicBlock;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.NullCheckInstruction;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NullCheckInsertionTest {

    private static final MethodReference RUN =
            new MethodReference("Example", "run", ValueType.object("Example"), ValueType.VOID);
    private static final MethodReference TOUCH =
            new MethodReference("Example", new MethodDescriptor("touch", ValueType.VOID));

    @Test
    void checksAgainInAHandlerThatTheTryMayLeaveEarly() {
        Program program = new Program();
        program.createVariable();
        Variable target = program.createVariable();
        BasicBlock start = program.createBasicBlock();
        BasicBlock body = program.createBasicBlock();
        BasicBlock handler = program.createBasicBlock();

        JumpInstruction enter = new JumpInstruction();
        enter.setTarget(body);
        start.add(enter);

        // a call that can throw comes before the first check
        body.add(staticCall());
        body.add(call(target));
        body.add(new ExitInstruction());
        TryCatchBlock tryCatch = new TryCatchBlock();
        tryCatch.setHandler(handler);
        body.getTryCatchBlocks().add(tryCatch);

        handler.setExceptionVariable(program.createVariable());
        handler.add(call(target));
        handler.add(new ExitInstruction());

        new NullCheckInsertion(NullCheckFilter.EMPTY).transformProgram(program, RUN);

        boolean checked = false;
        for (var instruction : handler) {
            checked |= instruction instanceof NullCheckInstruction check && check.getValue() == target;
        }
        assertTrue(checked, "the handler relies on a check the try block may never reach");
    }

    private static InvokeInstruction staticCall() {
        InvokeInstruction invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(TOUCH);
        return invoke;
    }

    private static InvokeInstruction call(Variable instance) {
        InvokeInstruction invoke = new InvokeInstruction();
        invoke.setType(InvocationType.VIRTUAL);
        invoke.setMethod(TOUCH);
        invoke.setInstance(instance);
        return invoke;
    }
}
