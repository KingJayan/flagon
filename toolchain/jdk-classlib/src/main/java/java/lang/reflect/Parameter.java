package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * a method's or constructor's parameter. the build sends getParameters here; names come from
 * what javac -parameters wrote, or are argN as on the jvm.
 */
public final class Parameter implements AnnotatedElement {

    private final Executable executable;
    private final int index;
    private final String name;
    private final boolean named;

    private Parameter(Executable executable, int index, String name, boolean named) {
        this.executable = executable;
        this.index = index;
        this.name = name;
        this.named = named;
    }

    public static Parameter[] of(Method method) {
        return read(method, method.getName(), method.getReturnType());
    }

    public static Parameter[] of(Constructor<?> constructor) {
        return read(constructor, "<init>", void.class);
    }

    public static Parameter[] of(Executable executable) {
        return executable instanceof Method method ? of(method)
                : of((Constructor<?>) executable);
    }

    private static Parameter[] read(Executable executable, String name, Class<?> result) {
        Class<?>[] types = executable.getParameterTypes();
        String key = KeptData.keyOf(name, types, result);
        String[] names = null;
        for (String[] parts : KeptData.of(executable.getDeclaringClass()).lines('A')) {
            if (parts[0].equals(key) && parts.length > 1) {
                names = parts[1].split(",", -1);
            }
        }
        Parameter[] parameters = new Parameter[types.length];
        for (int at = 0; at < types.length; at++) {
            boolean known = names != null && names.length == types.length && !names[at].isEmpty();
            parameters[at] = new Parameter(executable, at,
                    known ? KeptData.decode(names[at]) : "arg" + at, known);
        }
        return parameters;
    }

    public String getName() {
        return name;
    }

    public boolean isNamePresent() {
        return named;
    }

    public Executable getDeclaringExecutable() {
        return executable;
    }

    public int getModifiers() {
        return 0;
    }

    public Class<?> getType() {
        return executable.getParameterTypes()[index];
    }

    public Type getParameterizedType() {
        Type[] generic = executable.getGenericParameterTypes();
        return generic.length == executable.getParameterCount() ? generic[index] : getType();
    }

    public boolean isImplicit() {
        return false;
    }

    public boolean isSynthetic() {
        return false;
    }

    public boolean isVarArgs() {
        return executable.isVarArgs() && index == executable.getParameterCount() - 1;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        for (Annotation annotation : getDeclaredAnnotations()) {
            if (annotationClass.isInstance(annotation)) {
                return annotationClass.cast(annotation);
            }
        }
        return null;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        Annotation[][] all = executable.getParameterAnnotations();
        return index < all.length ? all[index].clone() : new Annotation[0];
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return getDeclaredAnnotationsByType(annotationClass);
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
        return getAnnotation(annotationClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        List<T> found = new ArrayList<>();
        for (Annotation annotation : getDeclaredAnnotations()) {
            if (annotationClass.isInstance(annotation)) {
                found.add(annotationClass.cast(annotation));
            }
        }
        return found.toArray((T[]) Array.newInstance(annotationClass, 0));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Parameter parameter && parameter.executable.equals(executable)
                && parameter.index == index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(executable, index);
    }

    @Override
    public String toString() {
        String type = getParameterizedType().getTypeName();
        if (isVarArgs()) {
            type = type.replaceFirst("\\[\\]$", "...");
        }
        return type + " " + name;
    }
}
