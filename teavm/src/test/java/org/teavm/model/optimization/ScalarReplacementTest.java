package org.teavm.model.optimization;

import org.junit.jupiter.api.Test;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReference;
import org.teavm.model.Incoming;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.PutFieldInstruction;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScalarReplacementTest {

    private static final FieldReference VALUE = new FieldReference("Example", "value");

    @Test
    void dropsANullCheckOnAMergeOfReplacedObjects() {
        Program program = new Program();
        program.createVariable();
        Variable choice = program.createVariable();
        BasicBlock start = program.createBasicBlock();
        BasicBlock left = program.createBasicBlock();
        BasicBlock right = program.createBasicBlock();
        BasicBlock merge = program.createBasicBlock();

        BranchingInstruction branch = new BranchingInstruction(BranchingCondition.EQUAL);
        branch.setOperand(choice);
        branch.setConsequent(left);
        branch.setAlternative(right);
        start.add(branch);

        Variable fromLeft = build(program, left, merge);
        Variable fromRight = build(program, right, merge);

        Variable merged = program.createVariable();
        Phi phi = new Phi();
        phi.setReceiver(merged);
        phi.getIncomings().add(incoming(left, fromLeft));
        phi.getIncomings().add(incoming(right, fromRight));
        merge.getPhis().add(phi);

        NullCheckInstruction check = new NullCheckInstruction();
        check.setReceiver(program.createVariable());
        check.setValue(merged);
        merge.add(check);
        merge.add(new ExitInstruction());

        new ScalarReplacement().optimize(context(), program);

        for (var instruction : merge) {
            assertTrue(!(instruction instanceof NullCheckInstruction), "the null check survived");
        }
    }

    private static Variable build(Program program, BasicBlock block, BasicBlock next) {
        Variable instance = program.createVariable();
        ConstructInstruction construct = new ConstructInstruction();
        construct.setReceiver(instance);
        construct.setType("Example");
        block.add(construct);

        Variable value = program.createVariable();
        IntegerConstantInstruction constant = new IntegerConstantInstruction();
        constant.setReceiver(value);
        constant.setConstant(1);
        block.add(constant);

        PutFieldInstruction put = new PutFieldInstruction();
        put.setInstance(instance);
        put.setField(VALUE);
        put.setFieldType(ValueType.INTEGER);
        put.setValue(value);
        block.add(put);

        JumpInstruction jump = new JumpInstruction();
        jump.setTarget(next);
        block.add(jump);
        return instance;
    }

    private static Incoming incoming(BasicBlock source, Variable value) {
        Incoming incoming = new Incoming();
        incoming.setSource(source);
        incoming.setValue(value);
        return incoming;
    }

    private static MethodOptimizationContext context() {
        ClassHolder owner = new ClassHolder("Example");
        MethodHolder method = new MethodHolder("merge", ValueType.INTEGER, ValueType.VOID);
        owner.addMethod(method);
        return new MethodOptimizationContext() {
            @Override
            public MethodReader getMethod() {
                return method;
            }

            @Override
            public DependencyInfo getDependencyInfo() {
                return null;
            }

            @Override
            public ClassReaderSource getClassSource() {
                return null;
            }

            @Override
            public ClassHierarchy getHierarchy() {
                return null;
            }
        };
    }
}
