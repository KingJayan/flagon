package org.teavm.backend.wasm.model.instruction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WasmTypeInferenceTest {

    @Test
    void countsTheStackUnderAnUntypedBlock() {
        var inference = run(new WasmInt32Constant(1), new WasmInt32Constant(2), new WasmBlock(false));

        assertEquals(2, inference.typeStack.size());
        assertEquals(2, inference.getDepthBeforeLastInstructionOut());
    }

    @Test
    void countsTheStackUnderAnUntypedConditional() {
        var inference = run(new WasmInt32Constant(1), new WasmInt32Constant(2), new WasmInt32Constant(3),
                new WasmDrop(), new WasmConditional());

        assertEquals(1, inference.typeStack.size());
        assertEquals(1, inference.getDepthBeforeLastInstructionOut());
    }

    @Test
    void countsTheStackUnderABranch() {
        var inference = run(new WasmInt32Constant(1), new WasmInt32Constant(2), new WasmInt32Constant(3),
                new WasmDrop(), new WasmBranch(new WasmInstructionList()));

        assertEquals(1, inference.typeStack.size());
        assertEquals(1, inference.getDepthBeforeLastInstructionOut());
    }

    @Test
    void countsNothingUnderABreak() {
        var inference = run(new WasmInt32Constant(1), new WasmInt32Constant(2),
                new WasmBreak(new WasmInstructionList()));

        assertEquals(0, inference.typeStack.size());
        assertEquals(0, inference.getDepthBeforeLastInstructionOut());
    }

    private static WasmTypeInference run(WasmInstruction... instructions) {
        var inference = new WasmTypeInference();
        for (var instruction : instructions) {
            instruction.acceptVisitor(inference);
        }
        return inference;
    }
}
