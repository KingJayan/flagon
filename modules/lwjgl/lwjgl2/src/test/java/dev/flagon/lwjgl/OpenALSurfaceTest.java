package dev.flagon.lwjgl;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenALSurfaceTest {

    private static final Set<String> NATIVE = Set.of(
            "ALC.create(Lorg/lwjgl/system/FunctionProviderLocal;)V",
            "ALC.getFunctionProvider()Lorg/lwjgl/system/FunctionProviderLocal;",
            "ALC.createCapabilities(JLjava/util/function/IntFunction;)"
                    + "Lorg/lwjgl/openal/ALCCapabilities;",
            "AL.createCapabilities(Lorg/lwjgl/openal/ALCCapabilities;"
                    + "Ljava/util/function/IntFunction;)Lorg/lwjgl/openal/ALCapabilities;",
            "ALCCapabilities.getAddressBuffer()Lorg/lwjgl/PointerBuffer;",
            "ALCapabilities.getAddressBuffer()Lorg/lwjgl/PointerBuffer;",
            "ALC10.alcGetCurrentContext()J");

    /** lwjgl 2 has 0 and lwjgl 3 has -1; a program carries the value it was compiled with */
    private static final Set<String> RENUMBERED = Set.of("ALC10.ALC_INVALID");

    @ParameterizedTest
    @ValueSource(strings = {"lwjgl2", "lwjgl3"})
    void everyPublicMemberIsHere(String version) throws Exception {
        List<String> missing = new ArrayList<>();
        try (ZipFile jar = new ZipFile(System.getProperty("flagon.openal." + version))) {
            for (var entry : jar.stream().toList()) {
                String path = entry.getName();
                if (!path.startsWith("org/lwjgl/openal/") || !path.endsWith(".class")
                        || path.contains("$")) {
                    continue;
                }
                ClassModel real = ClassFile.of().parse(jar.getInputStream(entry).readAllBytes());
                if ((real.flags().flagsMask() & ClassFile.ACC_PUBLIC) != 0) {
                    compare(real, missing);
                }
            }
        }
        assertEquals(List.of(), missing);
    }

    private static void compare(ClassModel real, List<String> missing) throws IOException {
        String name = real.thisClass().asInternalName().replace('/', '.');
        String simple = name.substring(name.lastIndexOf('.') + 1);
        if (!OPENAL.contains(simple)) {
            return;
        }
        Class<?> ours;
        try {
            ours = Class.forName(name);
        } catch (ClassNotFoundException absent) {
            missing.add(name);
            return;
        }
        Map<String, Method> methods = new HashMap<>();
        for (Method method : ours.getMethods()) {
            methods.put(method.getName() + MethodType.methodType(method.getReturnType(),
                    method.getParameterTypes()).toMethodDescriptorString(), method);
        }
        for (MethodModel method : real.methods()) {
            String key = method.methodName().stringValue() + method.methodType().stringValue();
            int flags = method.flags().flagsMask();
            if ((flags & ClassFile.ACC_PUBLIC) == 0 || key.startsWith("<")
                    || isNative(method.methodName().stringValue())
                    || NATIVE.contains(simple + "." + key)) {
                continue;
            }
            Method mine = methods.get(key);
            if (mine == null || Modifier.isStatic(mine.getModifiers())
                    != ((flags & ClassFile.ACC_STATIC) != 0)) {
                missing.add(simple + "." + key);
            }
        }
        for (FieldModel field : real.fields()) {
            if ((field.flags().flagsMask() & ClassFile.ACC_PUBLIC) == 0) {
                continue;
            }
            String fieldName = field.fieldName().stringValue();
            // a function address, which nothing here has
            if (field.fieldType().stringValue().equals("J")) {
                continue;
            }
            Field mine;
            try {
                mine = ours.getField(fieldName);
            } catch (NoSuchFieldException absent) {
                missing.add(simple + "." + fieldName);
                continue;
            }
            var constant = field.findAttribute(Attributes.constantValue());
            if (constant.isPresent() && !RENUMBERED.contains(simple + "." + fieldName)) {
                Object value = constant.get().constant().constantValue();
                try {
                    if (!value.equals(mine.get(null))) {
                        missing.add(simple + "." + fieldName + " = " + value);
                    }
                } catch (IllegalAccessException refused) {
                    throw new AssertionError(refused);
                }
            }
        }
    }

    /** lwjgl 3 names the raw native form of each call n plus the call, as nalGetError */
    private static boolean isNative(String method) {
        return method.startsWith("nal");
    }

    private static final Set<String> OPENAL = Set.of("AL", "AL10", "AL11", "ALC", "ALC10", "ALC11",
            "ALCCapabilities", "ALCapabilities", "ALCcontext", "ALCdevice", "OpenALException");
}
