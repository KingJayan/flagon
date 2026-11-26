package dev.flagon.platform;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** annotations by type, looking through the container of a repeatable one */
final class Annotations {

    private Annotations() {
    }

    @SuppressWarnings("unchecked")
    static <A extends Annotation> A[] byType(Annotation[] present, Class<A> asked) {
        List<A> found = new ArrayList<>();
        for (Annotation annotation : present) {
            if (asked.isInstance(annotation)) {
                found.add(asked.cast(annotation));
            }
        }
        Repeatable repeatable = asked.getAnnotation(Repeatable.class);
        if (found.isEmpty() && repeatable != null) {
            for (Annotation annotation : present) {
                if (repeatable.value().isInstance(annotation)) {
                    found.addAll(List.of(contents(annotation, asked)));
                }
            }
        }
        return found.toArray((A[]) Array.newInstance(asked, 0));
    }

    @SuppressWarnings("unchecked")
    private static <A extends Annotation> A[] contents(Annotation container, Class<A> asked) {
        try {
            Method value = container.annotationType().getMethod("value");
            return (A[]) value.invoke(container);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                 | ClassCastException unread) {
            return (A[]) Array.newInstance(asked, 0);
        }
    }
}
