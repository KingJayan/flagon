package dev.flagon.shims;

import org.teavm.asm.AnnotationVisitor;
import org.teavm.asm.ClassReader;
import org.teavm.asm.ClassVisitor;
import org.teavm.asm.FieldVisitor;
import org.teavm.asm.MethodVisitor;
import org.teavm.asm.Opcodes;
import org.teavm.asm.RecordComponentVisitor;
import org.teavm.asm.Type;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

final class ClassFacts {

    record Member(String name, String descriptor, int access) {
    }

    record Component(String name, String descriptor, String signature) {
    }

    record Parameters(String method, List<String> names) {
    }

    record Default(String method, String value) {
    }

    String name;
    int access;
    String signature;
    final List<String> interfaces = new ArrayList<>();
    final List<Member> fields = new ArrayList<>();
    final List<Member> methods = new ArrayList<>();
    final List<Component> components = new ArrayList<>();
    final List<String> permitted = new ArrayList<>();
    final List<Parameters> parameters = new ArrayList<>();
    final List<Default> defaults = new ArrayList<>();
    String enclosingOwner;
    String enclosingMethod;
    String enclosingDescriptor;
    boolean record;

    private ClassFacts() {
    }

    static ClassFacts read(byte[] bytes) {
        ClassFacts facts = new ClassFacts();
        new ClassReader(bytes).accept(facts.new Reader(), ClassReader.SKIP_CODE
                | ClassReader.SKIP_FRAMES);
        return facts;
    }

    boolean declares(String field) {
        return fields.stream().anyMatch(member -> member.name().equals(field));
    }

    long defaultSerialId() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF(name.replace('/', '.'));

            List<Member> declared = methods.stream()
                    .filter(method -> !method.name().equals("<init>")
                            && !method.name().equals("<clinit>"))
                    .toList();
            int classMods = access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL
                    | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT);
            if ((classMods & Opcodes.ACC_INTERFACE) != 0) {
                classMods = declared.isEmpty() ? classMods & ~Opcodes.ACC_ABSTRACT
                        : classMods | Opcodes.ACC_ABSTRACT;
            }
            out.writeInt(classMods);

            for (String implemented : interfaces.stream().map(i -> i.replace('/', '.')).sorted()
                    .toList()) {
                out.writeUTF(implemented);
            }

            int fieldMask = Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED
                    | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_VOLATILE
                    | Opcodes.ACC_TRANSIENT;
            for (Member field : fields.stream().sorted(Comparator.comparing(Member::name))
                    .toList()) {
                int mods = field.access() & fieldMask;
                if ((mods & Opcodes.ACC_PRIVATE) == 0
                        || (mods & (Opcodes.ACC_STATIC | Opcodes.ACC_TRANSIENT)) == 0) {
                    out.writeUTF(field.name());
                    out.writeInt(mods);
                    out.writeUTF(field.descriptor());
                }
            }

            if (methods.stream().anyMatch(method -> method.name().equals("<clinit>"))) {
                out.writeUTF("<clinit>");
                out.writeInt(Opcodes.ACC_STATIC);
                out.writeUTF("()V");
            }

            int methodMask = Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED
                    | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNCHRONIZED
                    | Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_STRICT;
            List<Member> constructors = methods.stream()
                    .filter(method -> method.name().equals("<init>"))
                    .sorted(Comparator.comparing(Member::descriptor)).toList();
            for (Member constructor : constructors) {
                int mods = constructor.access() & methodMask;
                if ((mods & Opcodes.ACC_PRIVATE) == 0) {
                    out.writeUTF("<init>");
                    out.writeInt(mods);
                    out.writeUTF(constructor.descriptor().replace('/', '.'));
                }
            }

            for (Member method : declared.stream()
                    .sorted(Comparator.comparing(Member::name).thenComparing(Member::descriptor))
                    .toList()) {
                int mods = method.access() & methodMask;
                if ((mods & Opcodes.ACC_PRIVATE) == 0) {
                    out.writeUTF(method.name());
                    out.writeInt(mods);
                    out.writeUTF(method.descriptor().replace('/', '.'));
                }
            }
            out.flush();

            byte[] hash = MessageDigest.getInstance("SHA").digest(bytes.toByteArray());
            long id = 0;
            for (int at = Math.min(hash.length, 8) - 1; at >= 0; at--) {
                id = (id << 8) | (hash[at] & 0xFF);
            }
            return id;
        } catch (IOException impossible) {
            throw new UncheckedIOException(impossible);
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }

    private final class Reader extends ClassVisitor {

        Reader() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int flags, String className, String classSignature,
                          String parent, String[] implemented) {
            name = className;
            access = flags;
            signature = classSignature;
            record = "java/lang/Record".equals(parent);
            interfaces.addAll(List.of(implemented));
        }

        @Override
        public void visitInnerClass(String inner, String outer, String simple, int flags) {
            if (inner.equals(name)) {
                // a member class's own flags live here; its class file says only public or not
                access = flags | (access & Opcodes.ACC_SUPER);
            }
        }

        @Override
        public void visitOuterClass(String owner, String method, String descriptor) {
            enclosingOwner = owner;
            enclosingMethod = method;
            enclosingDescriptor = descriptor;
        }

        @Override
        public void visitPermittedSubclass(String subclass) {
            permitted.add(subclass);
        }

        @Override
        public RecordComponentVisitor visitRecordComponent(String component, String descriptor,
                                                           String componentSignature) {
            components.add(new Component(component, descriptor, componentSignature));
            return null;
        }

        @Override
        public FieldVisitor visitField(int flags, String field, String descriptor,
                                                     String fieldSignature, Object value) {
            fields.add(new Member(field, descriptor, flags));
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int flags, String method, String descriptor,
                                         String methodSignature, String[] thrown) {
            methods.add(new Member(method, descriptor, flags));
            String key = method + descriptor;
            List<String> names = new ArrayList<>();
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitParameter(String parameter, int parameterFlags) {
                    names.add(parameter == null ? "" : parameter);
                }

                @Override
                public AnnotationVisitor visitAnnotationDefault() {
                    return new DefaultWriter(Type.getReturnType(descriptor), value -> {
                        if (value != null) {
                            defaults.add(new Default(key, value));
                        }
                    });
                }

                @Override
                public void visitEnd() {
                    if (!names.isEmpty()) {
                        parameters.add(new Parameters(key, names));
                    }
                }
            };
        }
    }

    /** writes a default as text, or null when it holds an annotation, which is not carried */
    private static final class DefaultWriter extends AnnotationVisitor {

        private final Type element;
        private final boolean array;
        private final Consumer<String> done;
        private final List<String> items = new ArrayList<>();
        private boolean unsupported;

        DefaultWriter(Type type, Consumer<String> done) {
            this(type.getSort() == Type.ARRAY ? type.getElementType() : type, false, done);
        }

        private DefaultWriter(Type element, boolean array, Consumer<String> done) {
            super(Opcodes.ASM9);
            this.element = element;
            this.array = array;
            this.done = done;
        }

        @Override
        public void visit(String key, Object value) {
            if (value.getClass().isArray()) {
                List<String> elements = new ArrayList<>();
                for (int at = 0; at < Array.getLength(value); at++) {
                    elements.add(ReflectionLines.literal(Array.get(value, at), element));
                }
                items.add("[" + String.join(",", elements) + "]");
            } else {
                items.add(ReflectionLines.literal(value, element));
            }
        }

        @Override
        public void visitEnum(String key, String descriptor, String value) {
            items.add("e" + descriptor + "." + ReflectionLines.encode(value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String key, String descriptor) {
            unsupported = true;
            return null;
        }

        @Override
        public AnnotationVisitor visitArray(String key) {
            return new DefaultWriter(element, true, text -> {
                if (text == null) {
                    unsupported = true;
                } else {
                    items.add(text);
                }
            });
        }

        @Override
        public void visitEnd() {
            if (unsupported) {
                done.accept(null);
            } else if (array) {
                done.accept("[" + String.join(",", items) + "]");
            } else if (items.size() == 1) {
                done.accept(items.get(0));
            }
        }
    }
}
