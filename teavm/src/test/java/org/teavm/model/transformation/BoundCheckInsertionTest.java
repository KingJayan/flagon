package org.teavm.model.transformation;

import org.junit.jupiter.api.Test;
import org.teavm.model.BasicBlock;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.BoundCheckInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoundCheckInsertionTest {

    @Test
    void checksAnIndexPastTheEndOfAnArrayOfAKnownSize() {
        assertEquals(1, boundChecks(5, 9));
    }

    @Test
    void checksAnIndexAtTheEndOfAnArrayOfAKnownSize() {
        assertEquals(1, boundChecks(5, 5));
    }

    @Test
    void leavesAnIndexInsideAnArrayOfAKnownSizeAlone() {
        assertEquals(0, boundChecks(5, 4));
    }

    private static int boundChecks(int size, int index) {
        Program program = new Program();
        program.createVariable();
        BasicBlock block = program.createBasicBlock();

        Variable sizeVar = program.createVariable();
        IntegerConstantInstruction sizeConstant = new IntegerConstantInstruction();
        sizeConstant.setReceiver(sizeVar);
        sizeConstant.setConstant(size);
        block.add(sizeConstant);

        Variable array = program.createVariable();
        ConstructArrayInstruction build = new ConstructArrayInstruction();
        build.setReceiver(array);
        build.setSize(sizeVar);
        build.setItemType(ValueType.INTEGER);
        block.add(build);

        Variable data = program.createVariable();
        UnwrapArrayInstruction unwrap = new UnwrapArrayInstruction(ArrayElementType.INT);
        unwrap.setReceiver(data);
        unwrap.setArray(array);
        block.add(unwrap);

        Variable indexVar = program.createVariable();
        IntegerConstantInstruction indexConstant = new IntegerConstantInstruction();
        indexConstant.setReceiver(indexVar);
        indexConstant.setConstant(index);
        block.add(indexConstant);

        Variable element = program.createVariable();
        GetElementInstruction read = new GetElementInstruction(ArrayElementType.INT);
        read.setReceiver(element);
        read.setArray(data);
        read.setIndex(indexVar);
        block.add(read);

        block.add(new ExitInstruction());

        new BoundCheckInsertion().transformProgram(program,
                new MethodReference("Example", "read", ValueType.INTEGER));

        int found = 0;
        for (var instruction : program.basicBlockAt(0)) {
            if (instruction instanceof BoundCheckInstruction) {
                found++;
            }
        }
        return found;
    }
}
