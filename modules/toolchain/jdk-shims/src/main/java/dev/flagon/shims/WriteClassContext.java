package dev.flagon.shims;

import org.teavm.model.AccessLevel;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

public final class WriteClassContext implements TeaVMPlugin, ClassHolderTransformer {

    private static final String SUBJECT = "java.lang.SecurityManager";
    private static final MethodDescriptor ASKED =
            MethodDescriptor.parse("getClassContext()[Ljava/lang/Class;");

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        if (!SUBJECT.equals(type.getName()) || type.getMethod(ASKED) != null) {
            return;
        }
        MethodHolder method = new MethodHolder(ASKED);
        method.setLevel(AccessLevel.PROTECTED);
        method.setProgram(noCallers());
        type.addMethod(method);
    }

    private static Program noCallers() {
        Program program = new Program();
        program.createVariable();
        BasicBlock block = program.createBasicBlock();

        Variable size = program.createVariable();
        IntegerConstantInstruction none = new IntegerConstantInstruction();
        none.setReceiver(size);
        none.setConstant(0);
        block.add(none);

        Variable callers = program.createVariable();
        ConstructArrayInstruction build = new ConstructArrayInstruction();
        build.setReceiver(callers);
        build.setItemType(ValueType.parse(Class.class));
        build.setSize(size);
        block.add(build);

        ExitInstruction give = new ExitInstruction();
        give.setValueToReturn(callers);
        block.add(give);
        return program;
    }
}
