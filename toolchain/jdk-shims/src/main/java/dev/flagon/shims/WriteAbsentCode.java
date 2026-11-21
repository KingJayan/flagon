package dev.flagon.shims;

import org.teavm.asm.ClassReader;
import org.teavm.asm.ClassVisitor;
import org.teavm.asm.Label;
import org.teavm.asm.MethodVisitor;
import org.teavm.asm.Opcodes;
import org.teavm.model.AccessLevel;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.reflection.AnnotationGenerationHelper;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class WriteAbsentCode implements TeaVMPlugin, ClassHolderTransformer {

    private static final String ABSENT = "java.lang.NoClassDefFoundError";
    private static final String UNLINKED = "java.lang.UnsatisfiedLinkError";

    private static final String[] OURS = {"java/", "javax/", "sun/", "org/teavm/", "dev/flagon/"};

    private record Member(String name, String descriptor, boolean isStatic) {
    }

    private final Map<String, Map<String, Member>> reached = new HashMap<>();
    private final Set<String> defined = new HashSet<>();
    private final Set<String> caught = new HashSet<>();
    private final Set<String> written = new HashSet<>();
    private ClassLoader loader;
    private boolean read;

    @Override
    public void install(TeaVMHost host) {
        loader = BuildClasspath.of(host);
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        // teavm writes the annotation data classes it generates as natives, then fills them in
        boolean theirs = !ours(type.getName().replace('.', '/'))
                && !type.getName().endsWith(AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX);
        if (theirs) {
            for (String implemented : type.getInterfaces()) {
                standIn(implemented, true, context);
            }
        }
        for (MethodHolder method : type.getMethods()) {
            Program program = method.getProgram();
            if (program == null) {
                if (theirs && method.hasModifier(ElementModifier.NATIVE)
                        && !method.getAnnotations().all().iterator().hasNext()) {
                    method.getModifiers().remove(ElementModifier.NATIVE);
                    method.setProgram(raise(UNLINKED, method.getName() + " has no native code in"
                            + " a page. see docs/COMPATIBILITY.md",
                            method.parameterCount()));
                }
                continue;
            }
            for (BasicBlock block : program.getBasicBlocks()) {
                for (Instruction instruction : block) {
                    read(instruction, context);
                }
                for (TryCatchBlock handler : block.getTryCatchBlocks()) {
                    if (handler.getExceptionType() != null) {
                        standIn(handler.getExceptionType(), context);
                    }
                }
            }
        }
    }

    private void read(Instruction instruction, ClassHolderTransformerContext context) {
        switch (instruction) {
            case InvokeInstruction invoke -> standIn(invoke.getMethod().getClassName(), context);
            case ConstructInstruction build -> standIn(build.getType(), context);
            case GetFieldInstruction get -> standIn(get.getField().getClassName(), context);
            case PutFieldInstruction put -> standIn(put.getField().getClassName(), context);
            case CastInstruction cast -> named(cast.getTargetType(), context);
            case IsInstanceInstruction check -> named(check.getType(), context);
            case ClassConstantInstruction constant -> named(constant.getConstant(), context);
            default -> {
            }
        }
    }

    private void named(ValueType type, ClassHolderTransformerContext context) {
        ValueType item = type;
        while (item instanceof ValueType.Array array) {
            item = array.getItemType();
        }
        if (item instanceof ValueType.Object object) {
            standIn(object.getClassName(), context);
        }
    }

    private void standIn(String name, ClassHolderTransformerContext context) {
        standIn(name, false, context);
    }

    private void standIn(String name, boolean contract, ClassHolderTransformerContext context) {
        scan();
        if (written.contains(name) || defined.contains(name) || ours(name.replace('.', '/'))
                || context.getHierarchy().getClassSource().get(name) != null
                || inner(name, context)) {
            return;
        }
        written.add(name);
        ClassHolder stand = new ClassHolder(name);
        stand.setLevel(AccessLevel.PUBLIC);
        if (contract) {
            stand.getModifiers().add(ElementModifier.INTERFACE);
            stand.getModifiers().add(ElementModifier.ABSTRACT);
            stand.setParent("java.lang.Object");
            context.submit(stand);
            return;
        }
        stand.setParent(caught.contains(name) ? "java.lang.RuntimeException" : "java.lang.Object");
        stand.addMethod(method(name, new Member("<init>", "()V", false)));
        for (Member member : reached.getOrDefault(name, Map.of()).values()) {
            if (member.name().equals("<init>") && member.descriptor().equals("()V")) {
                continue;
            }
            if (member.descriptor().startsWith("(")) {
                stand.addMethod(method(name, member));
            } else {
                stand.addField(field(member));
            }
        }
        context.submit(stand);
    }

    private static MethodHolder method(String name, Member member) {
        MethodDescriptor asked = MethodDescriptor.parse(member.name() + member.descriptor());
        MethodHolder method = new MethodHolder(asked);
        method.setLevel(AccessLevel.PUBLIC);
        if (member.isStatic()) {
            method.getModifiers().add(ElementModifier.STATIC);
        }
        method.setProgram(raise(ABSENT, name + " is not in this bundle. put the jar that carries"
                + " it in vendor/. see docs/COMPATIBILITY.md", asked.parameterCount()));
        return method;
    }

    private static FieldHolder field(Member member) {
        FieldHolder field = new FieldHolder(member.name());
        field.setLevel(AccessLevel.PUBLIC);
        field.setType(ValueType.parse(member.descriptor()));
        if (member.isStatic()) {
            field.getModifiers().add(ElementModifier.STATIC);
        }
        return field;
    }

    private static Program raise(String error, String said, int parameters) {
        Program program = new Program();
        for (int at = 0; at <= parameters; at++) {
            program.createVariable();
        }
        BasicBlock block = program.createBasicBlock();

        Variable thrown = program.createVariable();
        ConstructInstruction build = new ConstructInstruction();
        build.setReceiver(thrown);
        build.setType(error);
        block.add(build);

        Variable message = program.createVariable();
        StringConstantInstruction text = new StringConstantInstruction();
        text.setReceiver(message);
        text.setConstant(said);
        block.add(text);

        InvokeInstruction fill = new InvokeInstruction();
        fill.setType(InvocationType.SPECIAL);
        fill.setInstance(thrown);
        fill.setMethod(new MethodReference(error, "<init>", ValueType.parse(String.class),
                ValueType.VOID));
        fill.setArguments(message);
        block.add(fill);

        RaiseInstruction stop = new RaiseInstruction();
        stop.setException(thrown);
        block.add(stop);
        return program;
    }

    private static boolean ours(String owner) {
        for (String prefix : OURS) {
            if (owner.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean inner(String name, ClassHolderTransformerContext context) {
        int marker = name.indexOf('$');
        return marker > 0
                && context.getHierarchy().getClassSource().get(name.substring(0, marker)) != null;
    }

    private void scan() {
        if (read) {
            return;
        }
        read = true;
        List<byte[]> classes = classpath();
        for (byte[] bytes : classes) {
            defined.add(new ClassReader(bytes).getClassName().replace('/', '.'));
        }
        for (byte[] bytes : classes) {
            new ClassReader(bytes).accept(new Look(), ClassReader.SKIP_DEBUG
                    | ClassReader.SKIP_FRAMES);
        }
    }

    private final class Look extends ClassVisitor {

        Look() {
            super(Opcodes.ASM9);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] thrown) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitTryCatchBlock(Label start, Label end, Label handler,
                                               String type) {
                    if (type != null) {
                        caught.add(type.replace('/', '.'));
                    }
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String called,
                                            String asked, boolean isInterface) {
                    note(owner, called, asked, opcode == Opcodes.INVOKESTATIC);
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String called,
                                           String asked) {
                    note(owner, called, asked, opcode == Opcodes.GETSTATIC
                            || opcode == Opcodes.PUTSTATIC);
                }
            };
        }

        private void note(String owner, String called, String asked, boolean isStatic) {
            String name = owner.replace('/', '.');
            if (owner.startsWith("[") || ours(owner) || defined.contains(name)) {
                return;
            }
            reached.computeIfAbsent(name, absent -> new LinkedHashMap<>())
                    .putIfAbsent(called + asked, new Member(called, asked, isStatic));
        }
    }

    private List<byte[]> classpath() {
        if (!(loader instanceof URLClassLoader urls)) {
            throw new IllegalStateException("the build classpath cannot be read from "
                    + loader.getClass().getName());
        }
        List<byte[]> classes = new ArrayList<>();
        for (URL url : urls.getURLs()) {
            Path path = Path.of(url.getPath());
            try {
                if (Files.isDirectory(path)) {
                    readDirectory(path, classes);
                } else if (path.toString().endsWith(".jar")) {
                    readJar(path, classes);
                }
            } catch (IOException unreadable) {
                throw new UncheckedIOException(unreadable);
            }
        }
        return classes;
    }

    private static void readDirectory(Path path, List<byte[]> classes) throws IOException {
        try (Stream<Path> files = Files.walk(path)) {
            for (Path file : files.filter(at -> at.toString().endsWith(".class")).toList()) {
                classes.add(Files.readAllBytes(file));
            }
        }
    }

    private static void readJar(Path path, List<byte[]> classes) throws IOException {
        try (JarFile jar = new JarFile(path.toFile())) {
            for (JarEntry entry : jar.stream().toList()) {
                if (entry.getName().endsWith(".class")) {
                    try (InputStream bytes = jar.getInputStream(entry)) {
                        classes.add(bytes.readAllBytes());
                    }
                }
            }
        }
    }
}
