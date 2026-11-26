package dev.flagon.sandbox;

import dev.flagon.reflect.Reflectable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** types that carry what reflection reads from a class file beyond its members */
@Reflectable
final class ReflectedTypes {

    private ReflectedTypes() {
    }

    @Reflectable
    record Point(int x, String label) {
    }

    @Reflectable
    sealed interface Kind permits Big, Small {
    }

    @Reflectable
    record Big() implements Kind {
    }

    @Reflectable
    static final class Small implements Kind {
    }

    @Reflectable
    static class Box<T extends Comparable<T>> extends ArrayList<T>
            implements Supplier<Map<String, T[]>> {
        @Override
        public Map<String, T[]> get() {
            return null;
        }
    }

    @Reflectable
    @Retention(RetentionPolicy.RUNTIME)
    @interface Tagged {
        int weight() default 7;

        String[] names() default {"a", "b c"};

        Class<?> kind() default String.class;

        TimeUnit unit() default TimeUnit.SECONDS;
    }

    @Reflectable
    interface Named {
        default String name() {
            return "named";
        }
    }

    @Reflectable
    static class Holder {
        int count = 5;
        char letter = 'x';
        double scale;

        String place(String where, int times) {
            return where.repeat(times);
        }
    }

    static Object local() {
        @Reflectable
        class Inside {
        }
        return new Inside();
    }
}
