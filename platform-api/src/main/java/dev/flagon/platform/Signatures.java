package dev.flagon.platform;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * turns a class's generic signature, as the build kept it, into the types reflection hands out.
 */
final class Signatures {

    /** a class signature, read */
    record Read(TypeVariable<?>[] parameters, Type superclass, Type[] interfaces) {
    }

    private final Class<?> owner;
    private final ClassData data;
    private final String text;
    private final List<Variable> declared = new ArrayList<>();
    private int at;

    private Signatures(Class<?> owner, ClassData data, String text) {
        this.owner = owner;
        this.data = data;
        this.text = text;
    }

    static Read of(Class<?> type) {
        ClassData data = ClassData.of(type);
        if (data.signature == null) {
            return null;
        }
        return new Signatures(type, data, data.signature).classSignature();
    }

    /** a field type signature, with type variables looked up in the given class */
    static Type type(Class<?> owner, String signature) {
        return new Signatures(owner, ClassData.of(owner), signature).type();
    }

    private Read classSignature() {
        if (peek() == '<') {
            formals();
        }
        Type superclass = type();
        List<Type> interfaces = new ArrayList<>();
        while (at < text.length()) {
            interfaces.add(type());
        }
        return new Read(declared.toArray(new TypeVariable<?>[0]), owner.isInterface() ? null
                : superclass, interfaces.toArray(new Type[0]));
    }

    private void formals() {
        at++;
        while (peek() != '>') {
            int colon = text.indexOf(':', at);
            Variable variable = new Variable(owner, text.substring(at, colon));
            declared.add(variable);
            at = colon;
            List<Type> bounds = new ArrayList<>();
            while (peek() == ':') {
                at++;
                if (peek() == 'L' || peek() == 'T' || peek() == '[') {
                    bounds.add(new Lazy(this, at));
                    skipType();
                }
            }
            variable.bounds = bounds;
        }
        at++;
    }

    private Type type() {
        char kind = peek();
        if (kind == 'L') {
            return classType();
        } else if (kind == 'T') {
            int end = text.indexOf(';', at);
            String name = text.substring(at + 1, end);
            at = end + 1;
            return variable(name);
        } else if (kind == '[') {
            at++;
            Type item = type();
            if (item instanceof Class<?> plain) {
                return java.lang.reflect.Array.newInstance(plain, 0).getClass();
            }
            return new ArrayOf(item);
        }
        at++;
        return data.ofDescriptor(String.valueOf(kind));
    }

    private Type classType() {
        at++;
        StringBuilder name = new StringBuilder();
        Type outer = null;
        List<Type> arguments = new ArrayList<>();
        while (true) {
            char next = peek();
            if (next == '<') {
                arguments = arguments();
            } else if (next == '.' || next == ';') {
                at++;
                Class<?> raw = data.named(name.toString());
                Type made = arguments.isEmpty() && !(outer instanceof ParameterizedType)
                        ? raw : new Parameterized(raw, arguments.toArray(new Type[0]),
                                outer != null ? outer : raw.getDeclaringClass());
                if (next == ';') {
                    return made;
                }
                outer = made;
                arguments = new ArrayList<>();
                name.append('$');
            } else {
                name.append(next);
                at++;
            }
        }
    }

    private List<Type> arguments() {
        at++;
        List<Type> arguments = new ArrayList<>();
        while (peek() != '>') {
            char next = peek();
            if (next == '*') {
                at++;
                arguments.add(new Wildcard(new Type[] {Object.class}, new Type[0]));
            } else if (next == '+') {
                at++;
                arguments.add(new Wildcard(new Type[] {type()}, new Type[0]));
            } else if (next == '-') {
                at++;
                arguments.add(new Wildcard(new Type[] {Object.class}, new Type[] {type()}));
            } else {
                arguments.add(type());
            }
        }
        at++;
        return arguments;
    }

    private void skipType() {
        char kind = peek();
        if (kind == 'L') {
            int depth = 0;
            while (true) {
                char next = text.charAt(at++);
                if (next == '<') {
                    depth++;
                } else if (next == '>') {
                    depth--;
                } else if (next == ';' && depth == 0) {
                    return;
                }
            }
        } else if (kind == 'T') {
            at = text.indexOf(';', at) + 1;
        } else if (kind == '[') {
            at++;
            skipType();
        } else {
            at++;
        }
    }

    private TypeVariable<?> variable(String name) {
        for (Variable variable : declared) {
            if (variable.getName().equals(name)) {
                return variable;
            }
        }
        for (Class<?> around = owner.getEnclosingClass(); around != null;
             around = around.getEnclosingClass()) {
            for (TypeVariable<?> variable : Classes.getTypeParameters(around)) {
                if (variable.getName().equals(name)) {
                    return variable;
                }
            }
        }
        // a variable of the method a local class sits in, which nothing kept
        Variable free = new Variable(owner, name);
        free.bounds = List.of(Object.class);
        return free;
    }

    private char peek() {
        return text.charAt(at);
    }

    /** a bound read when first asked for, since it may name the variable that holds it */
    private static final class Lazy implements Type {

        private final Signatures from;
        private final int start;
        private Type read;

        Lazy(Signatures from, int start) {
            this.from = from;
            this.start = start;
        }

        Type resolve() {
            if (read == null) {
                Signatures again = new Signatures(from.owner, from.data, from.text);
                again.declared.addAll(from.declared);
                again.at = start;
                read = again.type();
            }
            return read;
        }
    }

    static final class Variable implements TypeVariable<Class<?>> {

        private final Class<?> declaration;
        private final String name;
        private List<Type> bounds = List.of();

        Variable(Class<?> declaration, String name) {
            this.declaration = declaration;
            this.name = name;
        }

        @Override
        public Type[] getBounds() {
            if (bounds.isEmpty()) {
                return new Type[] {Object.class};
            }
            Type[] resolved = new Type[bounds.size()];
            for (int at = 0; at < resolved.length; at++) {
                Type bound = bounds.get(at);
                resolved[at] = bound instanceof Lazy lazy ? lazy.resolve() : bound;
            }
            return resolved;
        }

        @Override
        public Class<?> getGenericDeclaration() {
            return declaration;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public java.lang.reflect.AnnotatedType[] getAnnotatedBounds() {
            return new java.lang.reflect.AnnotatedType[0];
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> type) {
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof TypeVariable<?> variable
                    && Objects.equals(name, variable.getName())
                    && Objects.equals(declaration, variable.getGenericDeclaration());
        }

        @Override
        public int hashCode() {
            return declaration.hashCode() ^ name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static final class Parameterized implements ParameterizedType {

        private final Class<?> raw;
        private final Type[] arguments;
        private final Type owner;

        Parameterized(Class<?> raw, Type[] arguments, Type owner) {
            this.raw = raw;
            this.arguments = arguments;
            this.owner = owner;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return arguments.clone();
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public Type getOwnerType() {
            return owner;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ParameterizedType type && raw.equals(type.getRawType())
                    && Objects.equals(owner, type.getOwnerType())
                    && Arrays.equals(arguments, type.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(arguments) ^ Objects.hashCode(owner) ^ raw.hashCode();
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder();
            if (owner != null) {
                out.append(owner.getTypeName()).append('$').append(raw.getSimpleName());
            } else {
                out.append(raw.getName());
            }
            if (arguments.length > 0) {
                out.append('<');
                for (int at = 0; at < arguments.length; at++) {
                    out.append(at == 0 ? "" : ", ").append(arguments[at].getTypeName());
                }
                out.append('>');
            }
            return out.toString();
        }
    }

    static final class Wildcard implements WildcardType {

        private final Type[] upper;
        private final Type[] lower;

        Wildcard(Type[] upper, Type[] lower) {
            this.upper = upper;
            this.lower = lower;
        }

        @Override
        public Type[] getUpperBounds() {
            return upper.clone();
        }

        @Override
        public Type[] getLowerBounds() {
            return lower.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof WildcardType type
                    && Arrays.equals(upper, type.getUpperBounds())
                    && Arrays.equals(lower, type.getLowerBounds());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(upper) ^ Arrays.hashCode(lower);
        }

        @Override
        public String toString() {
            if (lower.length > 0) {
                return "? super " + lower[0].getTypeName();
            }
            return upper.length == 0 || upper[0] == Object.class ? "?"
                    : "? extends " + upper[0].getTypeName();
        }
    }

    static final class ArrayOf implements GenericArrayType {

        private final Type item;

        ArrayOf(Type item) {
            this.item = item;
        }

        @Override
        public Type getGenericComponentType() {
            return item;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof GenericArrayType type
                    && item.equals(type.getGenericComponentType());
        }

        @Override
        public int hashCode() {
            return item.hashCode();
        }

        @Override
        public String toString() {
            return item.getTypeName() + "[]";
        }
    }
}
