package dev.flagon.lwjgl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** checks the gl es classes against lwjgl's own: the same calls and constants, and no others. */
class GlesSurfaceTest {

    /** classes that carry every call and constant of the real ones */
    private static final Set<String> WHOLE = Set.of("GLES20", "GLES30");

    @ParameterizedTest
    @ValueSource(strings = {"GLES20", "GLES30", "GLES", "GLESCapabilities"})
    void matchesLwjgl(String simple) throws Exception {
        String name = "org.lwjgl.opengles." + simple;
        ClassModel real;
        try (ZipFile jar = new ZipFile(System.getProperty("flagon.opengles"))) {
            var entry = jar.getEntry(name.replace('.', '/') + ".class");
            real = ClassFile.of().parse(jar.getInputStream(entry).readAllBytes());
        }
        Class<?> ours = Class.forName(name);
        List<String> wrong = new ArrayList<>();

        Set<String> realMembers = new HashSet<>();
        for (MethodModel method : real.methods()) {
            if ((method.flags().flagsMask() & ClassFile.ACC_PUBLIC) != 0) {
                realMembers.add(method.methodName().stringValue()
                        + method.methodType().stringValue());
            }
        }
        for (FieldModel field : real.fields()) {
            if ((field.flags().flagsMask() & ClassFile.ACC_PUBLIC) != 0) {
                realMembers.add(field.fieldName().stringValue());
            }
        }

        Set<String> ourMembers = new HashSet<>();
        for (Method method : ours.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                String key = method.getName() + MethodType.methodType(method.getReturnType(),
                        method.getParameterTypes()).toMethodDescriptorString();
                ourMembers.add(key);
                check(realMembers.contains(key), method, wrong);
            }
        }
        for (Field field : ours.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers())) {
                ourMembers.add(field.getName());
                check(realMembers.contains(field.getName()), field, wrong);
            }
        }

        if (WHOLE.contains(simple)) {
            for (MethodModel method : real.methods()) {
                String key = method.methodName().stringValue() + method.methodType().stringValue();
                // ngl calls take raw addresses, which programs reach only through the gl calls
                if (realMembers.contains(key) && !key.startsWith("ngl") && !key.startsWith("<")
                        && !ourMembers.contains(key)) {
                    wrong.add("missing " + simple + "." + key);
                }
            }
            for (FieldModel field : real.fields()) {
                var constant = field.findAttribute(Attributes.constantValue());
                if (constant.isPresent()) {
                    Object value = constant.get().constant().constantValue();
                    String fieldName = field.fieldName().stringValue();
                    Object mine = ourMembers.contains(fieldName)
                            ? ours.getField(fieldName).get(null) : null;
                    if (!value.equals(mine)) {
                        wrong.add(simple + "." + fieldName + " = " + value + ", here " + mine);
                    }
                }
            }
        }
        assertEquals(List.of(), wrong);
    }

    private static void check(boolean real, Member member, List<String> wrong) {
        if (!real) {
            wrong.add("not in lwjgl: " + member);
        }
    }
}
