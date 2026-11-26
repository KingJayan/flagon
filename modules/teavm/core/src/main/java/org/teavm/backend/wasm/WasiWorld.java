// flagon: the wit world a wasi component exports, written from the program's @Export methods
package org.teavm.backend.wasm;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.teavm.interop.Export;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.MethodHolder;
import org.teavm.model.ValueType;

final class WasiWorld {
    private static final Pattern KEBAB = Pattern.compile("[a-z][a-z0-9]*(-[a-z][a-z0-9]*)*");

    private WasiWorld() {
    }

    /** @throws IllegalStateException when an export name is not a wit identifier */
    static String write(ListableClassHolderSource classes) {
        var exports = new ArrayList<String>();
        for (var className : classes.getClassNames()) {
            if (className.startsWith("org.teavm.")) {
                continue;
            }
            for (MethodHolder method : classes.get(className).getMethods()) {
                var annotation = method.getAnnotations().get(Export.class.getName());
                if (annotation == null || !method.hasModifier(ElementModifier.STATIC)) {
                    continue;
                }
                var name = annotation.getValue("name").getString();
                if (name.equals("_start")) {
                    continue;
                }
                if (!KEBAB.matcher(name).matches()) {
                    throw new IllegalStateException("a wasi export is named in kebab-case, such as "
                            + "add-scores, but " + method.getReference() + " exports '" + name + "'");
                }
                exports.add("  export " + name + ": func(" + parameters(method.getParameterTypes()) + ")"
                        + result(method.getResultType()) + ";");
            }
        }
        exports.sort(null);
        return "package flagon:bundle;\n\nworld bundle {\n" + String.join("\n", exports)
                + (exports.isEmpty() ? "" : "\n") + "}\n";
    }

    private static String parameters(ValueType[] types) {
        List<String> written = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            written.add("p" + i + ": " + wit(types[i]));
        }
        return String.join(", ", written);
    }

    private static String result(ValueType type) {
        return type == ValueType.VOID ? "" : " -> " + wit(type);
    }

    private static String wit(ValueType type) {
        switch (((ValueType.Primitive) type).getKind()) {
            case BOOLEAN:
                return "bool";
            case BYTE:
                return "s8";
            case SHORT:
                return "s16";
            case CHARACTER:
                return "u16";
            case INTEGER:
                return "s32";
            case LONG:
                return "s64";
            case FLOAT:
                return "f32";
            default:
                return "f64";
        }
    }
}
