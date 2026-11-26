// flagon: under wasi, methods that would reach javascript run the java in Wasi instead
package org.teavm.backend.wasm.transformation;

import java.util.List;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;

public class WasiTransformation implements ClassHolderTransformer {
    private static final String WASI = "org.teavm.runtime.wasi.Wasi";
    private static final String QUEUE = "org.teavm.runtime.EventQueue";
    private static final String EVENT = "org.teavm.runtime.EventQueue$Event";
    /** each public method of INTL runs its namesake in INTL_ROOT, which has no javascript */
    private static final String INTL = "org.teavm.classlib.impl.unicode.Intl";
    private static final String INTL_ROOT = "org.teavm.classlib.impl.unicode.IntlRoot";

    /** a method, and the static method that takes its place; null for an empty body */
    private record Replacement(String className, MethodDescriptor method, MethodReference target) {
    }

    private static final List<Replacement> REPLACEMENTS = List.of(
            new Replacement("java.lang.System", new MethodDescriptor("nanoTime", long.class),
                    new MethodReference(WASI, "nanoTime", ValueType.LONG)),
            new Replacement("java.lang.Runtime", new MethodDescriptor("exit", int.class, void.class),
                    new MethodReference(WASI, "exit", ValueType.INTEGER, ValueType.VOID)),
            new Replacement(QUEUE, new MethodDescriptor("offerOutside", ValueType.object(EVENT),
                    ValueType.LONG, ValueType.INTEGER),
                    new MethodReference(QUEUE, "offerQueued", ValueType.object(EVENT), ValueType.LONG,
                            ValueType.INTEGER)),
            new Replacement(QUEUE, new MethodDescriptor("waitFor", long.class, void.class),
                    new MethodReference(WASI, "sleep", ValueType.LONG, ValueType.VOID)),
            new Replacement(QUEUE, new MethodDescriptor("interrupt", void.class), null),
            new Replacement("java.lang.Throwable", new MethodDescriptor("captureWasmGCStack", void.class),
                    null),
            new Replacement("java.lang.Throwable", new MethodDescriptor("readWasmGCStack", void.class),
                    null)
    );

    private boolean enabled;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (!enabled) {
            return;
        }
        if (cls.getName().equals(INTL)) {
            for (var method : cls.getMethods()) {
                if (method.hasModifier(ElementModifier.STATIC) && method.getLevel() == AccessLevel.PUBLIC) {
                    replace(method, new MethodReference(INTL_ROOT, method.getDescriptor()), context);
                }
            }
            return;
        }
        for (var replacement : REPLACEMENTS) {
            if (!replacement.className().equals(cls.getName())) {
                continue;
            }
            var method = cls.getMethod(replacement.method());
            if (method != null) {
                replace(method, replacement.target(), context);
            }
        }
    }

    private static void replace(MethodHolder method, MethodReference target,
            ClassHolderTransformerContext context) {
        method.getModifiers().remove(ElementModifier.NATIVE);
        for (var annotation : List.of("org.teavm.interop.Import", "org.teavm.interop.Intrinsified",
                "org.teavm.backend.javascript.spi.GeneratedBy")) {
            if (method.getAnnotations().get(annotation) != null) {
                method.getAnnotations().remove(annotation);
            }
        }
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        if (target == null) {
            pe.exit();
            return;
        }
        var parameters = method.getParameterTypes();
        var arguments = new ValueEmitter[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            arguments[i] = pe.var(i + 1, parameters[i]);
        }
        var result = pe.invoke(target, arguments);
        if (method.getResultType() == ValueType.VOID) {
            pe.exit();
        } else {
            result.returnValue();
        }
    }
}
