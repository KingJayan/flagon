package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * a record's component. the build sends Class.getRecordComponents here; the names and their
 * order come from what the build kept of the record, or from its fields when it kept nothing.
 */
public final class RecordComponent implements AnnotatedElement {

    private final Class<?> declaringRecord;
    private final String name;
    private final Class<?> type;
    private final String signature;
    private final Method accessor;
    private final Field field;

    private RecordComponent(Class<?> declaringRecord, String name, Class<?> type,
                            String signature) {
        this.declaringRecord = declaringRecord;
        this.name = name;
        this.type = type;
        this.signature = signature;
        this.accessor = find(() -> declaringRecord.getDeclaredMethod(name));
        this.field = find(() -> declaringRecord.getDeclaredField(name));
    }

    /** the components of a record, or null for any other class */
    public static RecordComponent[] of(Class<?> record) {
        if (record.getSuperclass() != Record.class) {
            return null;
        }
        KeptData kept = KeptData.of(record);
        List<RecordComponent> components = new ArrayList<>();
        List<String[]> written = kept.lines('R');
        if (!written.isEmpty()) {
            for (String[] parts : written) {
                components.add(new RecordComponent(record, parts[0], kept.ofDescriptor(parts[1]),
                        parts[2].equals("-") ? null : parts[2]));
            }
        } else {
            for (Field instance : record.getDeclaredFields()) {
                if (!Modifier.isStatic(instance.getModifiers())) {
                    components.add(new RecordComponent(record, instance.getName(),
                            instance.getType(), null));
                }
            }
        }
        return components.toArray(new RecordComponent[0]);
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public String getGenericSignature() {
        return signature;
    }

    public Type getGenericType() {
        if (accessor != null) {
            return accessor.getGenericReturnType();
        }
        return field != null ? field.getGenericType() : type;
    }

    public Method getAccessor() {
        return accessor;
    }

    public Class<?> getDeclaringRecord() {
        return declaringRecord;
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
        if (field != null) {
            return field.getDeclaredAnnotations();
        }
        return accessor != null ? accessor.getDeclaredAnnotations() : new Annotation[0];
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
    public String toString() {
        return type.getTypeName() + " " + name;
    }

    private interface Lookup<T> {
        T find() throws ReflectiveOperationException;
    }

    private static <T> T find(Lookup<T> lookup) {
        try {
            return lookup.find();
        } catch (ReflectiveOperationException absent) {
            return null;
        }
    }
}
