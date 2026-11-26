package dev.flagon.shims;

import org.teavm.extension.spi.GlobMatch;
import org.teavm.model.AccessLevel;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WriteReflectionData implements TeaVMPlugin, ClassHolderTransformer {

    public static final String METHOD = "$flagon$reflection";

    private static final String REFLECTABLE = "dev.flagon.reflect.Reflectable";
    private static final String PATTERNS = "flagon.reflect.classes";
    private static final String[] NOT_OURS = {"java.", "javax.", "jdk.", "sun.", "org.teavm.",
        "dev.flagon.platform.", "dev.flagon.runtime."};
    private static final MethodDescriptor DESCRIPTOR = new MethodDescriptor(METHOD,
            ValueType.parse(Object[].class));
    /** the parameter of the constructor java.io.ObjectInputStream calls */
    public static final String UNCONSTRUCTED = "dev.flagon.serial.Unconstructed";
    private static final String SERIALIZABLE = "java.io.Serializable";
    private static final String EXTERNALIZABLE = "java.io.Externalizable";

    private final List<String> patterns = new ArrayList<>();
    private ClassLoader loader;

    @Override
    public void install(TeaVMHost host) {
        loader = BuildClasspath.of(host);
        String listed = host.getProperties().getProperty(PATTERNS, "");
        for (String pattern : listed.split("[,\\s]+")) {
            if (!pattern.isEmpty()) {
                patterns.add(pattern);
            }
        }
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        String name = type.getName();
        if (notOurs(name) || type.getMethod(DESCRIPTOR) != null) {
            return;
        }
        boolean full = reflectable(type);
        ClassHierarchy hierarchy = context.getHierarchy();
        boolean serial = !type.hasModifier(ElementModifier.INTERFACE)
                && hierarchy.isSuperType(SERIALIZABLE, name, false);
        if (!full && !serial) {
            return;
        }
        byte[] bytes = read(name);
        if (bytes == null) {
            return;
        }
        ClassFacts facts = ClassFacts.read(bytes);
        Set<String> classes = new LinkedHashSet<>();
        String data = ReflectionLines.write(facts, full, serial, classes);
        if (!data.isEmpty()) {
            type.addMethod(method(data, classes, context));
        }
        if (serial && !facts.record && !type.hasModifier(ElementModifier.ABSTRACT)
                && !type.hasModifier(ElementModifier.ENUM)
                && !hierarchy.isSuperType(EXTERNALIZABLE, name, false)) {
            MethodHolder constructor = serialConstructor(type, hierarchy);
            if (constructor != null) {
                type.addMethod(constructor);
            }
        }
    }

    /**
     * runs the no-argument constructor of the nearest superclass that is not serializable, or
     * that the jdk classlib supplies, so its insides are made, and nothing of the classes between
     */
    private static MethodHolder serialConstructor(ClassHolder type, ClassHierarchy hierarchy) {
        String parent = type.getParent();
        while (parent != null && !parent.startsWith("java.")
                && hierarchy.isSuperType(SERIALIZABLE, parent, false)) {
            ClassReader above = hierarchy.getClassSource().get(parent);
            parent = above == null ? null : above.getParent();
        }
        if (parent == null) {
            return null;
        }
        ClassReader made = hierarchy.getClassSource().get(parent);
        MethodReader plain = made == null ? null
                : made.getMethod(new MethodDescriptor("<init>", ValueType.VOID));
        if (plain == null) {
            return null;
        }
        MethodHolder constructor = new MethodHolder("<init>", ValueType.object(UNCONSTRUCTED),
                ValueType.VOID);
        constructor.setLevel(AccessLevel.PUBLIC);
        constructor.getModifiers().add(ElementModifier.SYNTHETIC);
        Program program = new Program();
        Variable self = program.createVariable();
        program.createVariable();
        BasicBlock block = program.createBasicBlock();
        InvokeInstruction call = new InvokeInstruction();
        call.setType(InvocationType.SPECIAL);
        call.setInstance(self);
        call.setMethod(new MethodReference(parent, "<init>", ValueType.VOID));
        block.add(call);
        block.add(new ExitInstruction());
        constructor.setProgram(program);
        return constructor;
    }

    private boolean reflectable(ClassHolder type) {
        if (type.getAnnotations().get(REFLECTABLE) != null) {
            return true;
        }
        for (String pattern : patterns) {
            if (GlobMatch.match(pattern, type.getName())) {
                return true;
            }
        }
        return false;
    }

    private static MethodHolder method(String data, Set<String> classes,
                                       ClassHolderTransformerContext context) {
        MethodHolder method = new MethodHolder(DESCRIPTOR);
        method.setLevel(AccessLevel.PUBLIC);
        method.getModifiers().add(ElementModifier.STATIC);
        method.getModifiers().add(ElementModifier.SYNTHETIC);
        ProgramEmitter emitter = ProgramEmitter.create(method, context.getHierarchy());
        ValueEmitter array = emitter.constructArray(Object.class, classes.size() + 1);
        array.setElement(0, emitter.constant(data));
        int at = 1;
        for (String internal : classes) {
            array.setElement(at++, emitter.constant(ValueType.object(internal.replace('/', '.'))));
        }
        array.returnValue();
        return method;
    }

    private byte[] read(String name) {
        try (InputStream in = loader.getResourceAsStream(name.replace('.', '/') + ".class")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    private static boolean notOurs(String name) {
        for (String prefix : NOT_OURS) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
