package dev.flagon.types;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** renders the typescript declarations of a teavm es module from its class files */
final class Declarations {

    private static final String JSO = "org/teavm/jso/";
    private static final String JS_OBJECT = JSO + "JSObject";

    private static final Map<String, String> KNOWN = Map.ofEntries(
            Map.entry("java/lang/String", "string"),
            Map.entry(JS_OBJECT, "unknown"),
            Map.entry(JSO + "core/JSArray", "unknown[]"),
            Map.entry(JSO + "core/JSArrayReader", "unknown[]"),
            Map.entry(JSO + "core/JSBigInt", "bigint"),
            Map.entry(JSO + "core/JSBoolean", "boolean"),
            Map.entry(JSO + "core/JSDate", "Date"),
            Map.entry(JSO + "core/JSError", "Error"),
            Map.entry(JSO + "core/JSFunction", "Function"),
            Map.entry(JSO + "core/JSMap", "Map<unknown, unknown>"),
            Map.entry(JSO + "core/JSNumber", "number"),
            Map.entry(JSO + "core/JSPromise", "Promise<unknown>"),
            Map.entry(JSO + "core/JSRegExp", "RegExp"),
            Map.entry(JSO + "core/JSString", "string"),
            Map.entry(JSO + "core/JSSymbol", "symbol"),
            Map.entry(JSO + "core/JSUndefined", "undefined"));

    /** what teavm passes for an array marked @JSByRef */
    private static final Map<String, String> TYPED_ARRAYS = Map.of(
            "B", "Int8Array", "S", "Int16Array", "C", "Uint16Array", "I", "Int32Array",
            "J", "BigInt64Array", "F", "Float32Array", "D", "Float64Array");

    /** names typescript refuses for a parameter or a function of a module */
    private static final Set<String> RESERVED = Set.of(
            "arguments", "await", "debugger", "delete", "eval", "export", "function", "in", "let",
            "typeof", "var", "with", "yield");

    private final ClassLoader classes;
    private final Map<String, Optional<ClassModel>> loaded = new HashMap<>();
    private final Map<String, Boolean> javaScript = new HashMap<>();
    private final Map<String, String> declared = new HashMap<>();
    private final List<String> queue = new ArrayList<>();
    private final Set<String> names = new HashSet<>();
    private String entry;

    Declarations(ClassLoader classes) {
        this.classes = classes;
    }

    /** @return the declarations of the module whose entry point is mainClass */
    String module(String mainClass) {
        entry = mainClass.replace('.', '/');
        ClassModel main = load(entry).orElseThrow(
                () -> new IllegalArgumentException("no class file for " + mainClass));
        var out = new StringBuilder("// generated from ").append(mainClass).append('\n');
        out.append("export function main(args: string[]): void;\n");
        annotation(main, "JSExportClasses").ifPresent(exported -> {
            for (var value : values(exported, "value")) {
                if (value instanceof AnnotationValue.OfClass cls) {
                    String name = internal(cls.classSymbol());
                    if (exportsMembers(name)) {
                        declare(name);
                    }
                }
            }
        });
        for (var method : main.methods()) {
            if (isStatic(method) && kind(method) == Kind.METHOD
                    && annotation(method, "JSExport").isPresent()) {
                function(out, method);
            }
        }
        // a declaration can name types that need declarations of their own
        for (int i = 0; i < queue.size(); i++) {
            out.append('\n');
            ClassModel cls = load(queue.get(i)).orElseThrow();
            if (isJavaScript(queue.get(i))) {
                jsInterface(out, cls);
            } else {
                javaClass(out, cls);
            }
        }
        return out.toString();
    }

    private void function(StringBuilder out, MethodModel method) {
        String name = alias(method);
        String signature = "(" + parameters(method) + "): " + returnType(method) + ";\n";
        if (RESERVED.contains(name)) {
            out.append("declare function ").append(name).append('_').append(signature);
            out.append("export { ").append(name).append("_ as ").append(name).append(" };\n");
        } else {
            out.append("export function ").append(name).append(signature);
        }
    }

    /** a java class with @JSExport members, which the module exports by name */
    private void javaClass(StringBuilder out, ClassModel cls) {
        out.append("export class ").append(declared.get(name(cls))).append(" {\n");
        var members = new Members();
        boolean constructed = false;
        for (var method : cls.methods()) {
            if (annotation(method, "JSExport").isEmpty()) {
                continue;
            }
            if (method.methodName().equalsString("<init>")) {
                out.append("    constructor(").append(parameters(method)).append(");\n");
                constructed = true;
            } else if (isStatic(method)) {
                members.add(method, "static ");
            }
        }
        if (!constructed) {
            out.append("    private constructor();\n");
        }
        for (ClassModel at = cls; at != null; at = superclass(at)) {
            for (var method : at.methods()) {
                if (!isStatic(method) && !method.methodName().stringValue().startsWith("<")
                        && annotation(method, "JSExport").isPresent()) {
                    members.add(method, "");
                }
            }
        }
        members.render(out);
        out.append("}\n");
    }

    /** a javascript object that java declares as an interface or an abstract class */
    private void jsInterface(StringBuilder out, ClassModel cls) {
        out.append("export interface ").append(declared.get(name(cls)));
        var parents = parents(cls)
                .filter(parent -> !parent.equals(JS_OBJECT) && isJavaScript(parent))
                .map(parent -> type(ClassDesc.ofInternalName(parent), false))
                .filter(type -> type.matches("[A-Za-z_$][\\w$]*"))
                .toList();
        if (!parents.isEmpty()) {
            out.append(" extends ").append(String.join(", ", parents));
        }
        out.append(" {\n");
        var members = new Members();
        for (var method : cls.methods()) {
            boolean declaredOnly = method.flags().has(AccessFlag.ABSTRACT)
                    || method.flags().has(AccessFlag.NATIVE);
            if (declaredOnly && !isStatic(method) && annotation(method, "JSBody").isEmpty()) {
                members.add(method, "");
            }
        }
        members.render(out);
        out.append("}\n");
    }

    /** the properties and methods of one declaration, a property merged from its accessors */
    private final class Members {
        private final Map<String, String[]> properties = new LinkedHashMap<>();
        private final List<String> methods = new ArrayList<>();

        void add(MethodModel method, String prefix) {
            var types = method.methodTypeSymbol();
            switch (kind(method)) {
                case GETTER -> property(prefix + alias(method), returnType(method), false);
                case SETTER -> property(prefix + alias(method), type(types.parameterType(0),
                        byRef(method, 0)), true);
                case INDEXER -> {
                    boolean setter = types.parameterCount() == 2;
                    String key = type(types.parameterType(0), false);
                    String value = setter ? type(types.parameterType(1), false)
                            : returnType(method);
                    property("[key: " + key + "]", value, setter);
                }
                case METHOD -> methods.add(prefix + alias(method) + "(" + parameters(method)
                        + "): " + returnType(method) + ";");
            }
        }

        private void property(String name, String type, boolean writable) {
            String[] known = properties.computeIfAbsent(name,
                    n -> new String[] {type, "readonly "});
            if (writable) {
                known[1] = "";
            }
        }

        void render(StringBuilder out) {
            properties.forEach((name, property) -> {
                int split = name.startsWith("static ") ? "static ".length() : 0;
                out.append("    ").append(name, 0, split).append(property[1])
                        .append(name.substring(split)).append(": ").append(property[0])
                        .append(";\n");
            });
            methods.forEach(method -> out.append("    ").append(method).append('\n'));
        }
    }

    private enum Kind { METHOD, GETTER, SETTER, INDEXER }

    private static Kind kind(MethodModel method) {
        if (annotation(method, "JSIndexer").isPresent()) {
            return Kind.INDEXER;
        }
        if (annotation(method, "JSProperty").isEmpty()) {
            return Kind.METHOD;
        }
        return method.methodTypeSymbol().parameterCount() == 0 ? Kind.GETTER : Kind.SETTER;
    }

    /** the name javascript sees, by the rules of teavm's JSObjectClassTransformer */
    private static String alias(MethodModel method) {
        String name = method.methodName().stringValue();
        Kind kind = kind(method);
        String annotated = annotation(method, kind == Kind.METHOD ? "JSMethod" : "JSProperty")
                .map(found -> string(found, "value")).orElse("");
        if (!annotated.isEmpty() || kind == Kind.METHOD) {
            return annotated.isEmpty() ? name : annotated;
        }
        boolean flag = method.methodTypeSymbol().returnType().descriptorString().equals("Z");
        String prefix = kind == Kind.SETTER ? "set" : flag ? "is" : "get";
        if (name.length() > prefix.length() && name.startsWith(prefix)) {
            name = name.substring(prefix.length());
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    private String parameters(MethodModel method) {
        var types = method.methodTypeSymbol().parameterList();
        var names = parameterNames(method);
        boolean varargs = method.flags().has(AccessFlag.VARARGS);
        var out = new StringBuilder();
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            String name = names.get(i);
            if (varargs && i == types.size() - 1) {
                out.append("...");
            }
            out.append(RESERVED.contains(name) ? name + "_" : name).append(": ")
                    .append(type(types.get(i), byRef(method, i)));
        }
        return out.toString();
    }

    private static List<String> parameterNames(MethodModel method) {
        int count = method.methodTypeSymbol().parameterCount();
        var names = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            names.add("p" + i);
        }
        var listed = method.findAttribute(Attributes.methodParameters());
        if (listed.isPresent() && listed.get().parameters().size() == count) {
            for (int i = 0; i < count; i++) {
                int at = i;
                listed.get().parameters().get(i).name()
                        .ifPresent(name -> names.set(at, name.stringValue()));
            }
            return names;
        }
        var locals = method.code()
                .flatMap(code -> code.findAttribute(Attributes.localVariableTable()));
        if (locals.isPresent()) {
            int slot = isStatic(method) ? 0 : 1;
            for (int i = 0; i < count; i++) {
                int at = slot;
                int index = i;
                locals.get().localVariables().stream()
                        .filter(local -> local.slot() == at && local.startPc() == 0)
                        .findFirst()
                        .ifPresent(local -> names.set(index, local.name().stringValue()));
                String descriptor = method.methodTypeSymbol().parameterType(i).descriptorString();
                slot += descriptor.equals("J") || descriptor.equals("D") ? 2 : 1;
            }
        }
        return names;
    }

    private String returnType(MethodModel method) {
        boolean byRef = annotation(method, "JSByRef").isPresent();
        return type(method.methodTypeSymbol().returnType(), byRef);
    }

    private static boolean byRef(MethodModel method, int parameter) {
        var lists = new ArrayList<List<Annotation>>();
        method.findAttribute(Attributes.runtimeVisibleParameterAnnotations())
                .ifPresent(found -> lists.add(at(found.parameterAnnotations(), parameter)));
        method.findAttribute(Attributes.runtimeInvisibleParameterAnnotations())
                .ifPresent(found -> lists.add(at(found.parameterAnnotations(), parameter)));
        return lists.stream().flatMap(List::stream).anyMatch(found ->
                found.classSymbol().descriptorString().equals("L" + JSO + "JSByRef;"));
    }

    private static List<Annotation> at(List<List<Annotation>> lists, int index) {
        return index < lists.size() ? lists.get(index) : List.of();
    }

    private String type(ClassDesc type, boolean byRef) {
        if (type.isPrimitive()) {
            return switch (type.descriptorString()) {
                case "V" -> "void";
                case "Z" -> "boolean";
                case "J" -> "bigint";
                default -> "number";
            };
        }
        if (type.isArray()) {
            ClassDesc item = type.componentType();
            if (byRef && TYPED_ARRAYS.containsKey(item.descriptorString())) {
                return TYPED_ARRAYS.get(item.descriptorString());
            }
            String inner = type(item, false);
            return inner.matches("[\\w$.<>, \\[\\]]+") ? inner + "[]" : "(" + inner + ")[]";
        }
        String name = internal(type);
        if (KNOWN.containsKey(name)) {
            return KNOWN.get(name);
        }
        if (isJavaScript(name)) {
            ClassModel cls = load(name).orElseThrow();
            if (annotation(cls, "JSFunctor").isPresent()) {
                return functor(cls);
            }
            return name.startsWith(JSO) ? jsName(cls) : declare(name);
        }
        return exportsMembers(name) ? declare(name) : "unknown";
    }

    private String functor(ClassModel cls) {
        return cls.methods().stream()
                .filter(method -> method.flags().has(AccessFlag.ABSTRACT))
                .findFirst()
                .map(method -> "(" + parameters(method) + ") => " + returnType(method))
                .orElse("Function");
    }

    /** @return the name a declaration of the class goes by, queueing it the first time */
    private String declare(String name) {
        return declared.computeIfAbsent(name, key -> {
            String base = jsName(load(key).orElseThrow());
            String unique = base;
            for (int i = 2; !names.add(unique); i++) {
                unique = base + i;
            }
            queue.add(key);
            return unique;
        });
    }

    private static String jsName(ClassModel cls) {
        String named = annotation(cls, "JSClass").map(found -> string(found, "name")).orElse("");
        if (!named.isEmpty()) {
            return named;
        }
        String name = name(cls);
        return name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('$')) + 1);
    }

    /** whether teavm exports a java class from the module, as it has @JSExport members */
    private boolean exportsMembers(String name) {
        if (name.equals(entry) || isJavaScript(name)) {
            return false;
        }
        for (var cls = load(name).orElse(null); cls != null; cls = superclass(cls)) {
            if (cls.methods().stream().anyMatch(m -> annotation(m, "JSExport").isPresent())) {
                return true;
            }
        }
        return false;
    }

    /** whether the class is a javascript object, by the rules of teavm's JSTypeHelper */
    private boolean isJavaScript(String name) {
        if (name.equals(JS_OBJECT)) {
            return true;
        }
        Boolean known = javaScript.get(name);
        if (known != null) {
            return known;
        }
        boolean result = load(name).map(cls -> {
            boolean declaresOnly = cls.flags().has(AccessFlag.INTERFACE)
                    || cls.flags().has(AccessFlag.ABSTRACT)
                    || annotation(cls, "JSClass").isPresent();
            return declaresOnly && parents(cls).anyMatch(this::isJavaScript);
        }).orElse(false);
        javaScript.put(name, result);
        return result;
    }

    private static Stream<String> parents(ClassModel cls) {
        return Stream.concat(cls.superclass().stream(), cls.interfaces().stream())
                .map(ClassEntry::asInternalName);
    }

    private ClassModel superclass(ClassModel cls) {
        return cls.superclass().flatMap(parent -> load(parent.asInternalName())).orElse(null);
    }

    private Optional<ClassModel> load(String name) {
        return loaded.computeIfAbsent(name, key -> {
            try (InputStream in = classes.getResourceAsStream(key + ".class")) {
                return in == null ? Optional.empty()
                        : Optional.of(ClassFile.of().parse(in.readAllBytes()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static Optional<Annotation> annotation(AttributedElement element,
            String simpleName) {
        String descriptor = "L" + JSO + simpleName + ";";
        var all = new ArrayList<Annotation>();
        element.findAttribute(Attributes.runtimeInvisibleAnnotations())
                .ifPresent(found -> all.addAll(found.annotations()));
        element.findAttribute(Attributes.runtimeVisibleAnnotations())
                .ifPresent(found -> all.addAll(found.annotations()));
        return all.stream()
                .filter(found -> found.classSymbol().descriptorString().equals(descriptor))
                .findFirst();
    }

    private static List<AnnotationValue> values(Annotation annotation,
            String element) {
        return annotation.elements().stream()
                .filter(found -> found.name().equalsString(element))
                .map(found -> found.value())
                .flatMap(value -> value instanceof AnnotationValue.OfArray array
                        ? array.values().stream() : Stream.of(value))
                .toList();
    }

    private static String string(Annotation annotation, String element) {
        for (var value : values(annotation, element)) {
            if (value instanceof AnnotationValue.OfString text) {
                return text.stringValue();
            }
        }
        return "";
    }

    private static boolean isStatic(MethodModel method) {
        return method.flags().has(AccessFlag.STATIC);
    }

    private static String name(ClassModel cls) {
        return cls.thisClass().asInternalName();
    }

    private static String internal(ClassDesc type) {
        String descriptor = type.descriptorString();
        return descriptor.substring(1, descriptor.length() - 1);
    }
}
