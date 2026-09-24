package dev.flagon.sandbox;

import dev.flagon.platform.Clock;
import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.platform.UnsupportedCapabilityException;
import dev.flagon.runtime.Program;
import org.lwjgl.BufferUtils;
import org.lwjgl.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * checks that java semantics survive aot compilation to the browser
 */
public final class RuntimeSelfTestProgram implements Program {

    private final List<String> failures = new ArrayList<>();
    private int checks;

    @Override
    public String id() {
        return "selftest";
    }

    @Override
    public String description() {
        return "verifies core java semantics in the browser";
    }

    @Override
    public void run(String[] arguments) {
        ConsoleSink console = Platforms.get().console();
        failures.clear();
        checks = 0;

        integerArithmetic();
        floatingPoint();
        strings();
        arrays();
        collections();
        exceptions();
        polymorphism();
        lambdasAndClosures();
        clockBehaviour();
        nioBuffers();
        reflection();
        optedInReflection();
        classDefinition();
        entryPointArguments(arguments);

        console.info("selftest: " + (checks - failures.size()) + "/" + checks + " checks passed");
        if (!failures.isEmpty()) {
            for (String failure : failures) {
                console.error("selftest failure: " + failure, null);
            }
            throw new AssertionError(failures.size() + " of " + checks
                    + " self-test checks failed");
        }
    }

    private void integerArithmetic() {
        check("int add", 7, 3 + 4);
        check("int div truncation", -2, -7 / 3);
        check("int mod sign", -1, -7 % 3);
        check("int overflow wraps", Integer.MIN_VALUE, Integer.MAX_VALUE + 1);
        check("long math", 9_007_199_254_740_993L, (1L << 53) + 1L);
        check("long overflow wraps", Long.MIN_VALUE, Long.MAX_VALUE + 1L);
        check("unsigned shift", 1, -1 >>> 31);
        check("signed shift", -1, -1 >> 31);
        check("byte narrowing", (byte) -1, (byte) 255);
        check("char arithmetic", 'C', (char) ('A' + 2));
        check("integer parse", 1234, Integer.parseInt("1234"));
        check("long division", 3L, 10L / 3L);
    }

    private void floatingPoint() {
        check("double add", 0.30000000000000004d, 0.1d + 0.2d);
        check("float precision", 1.0f / 3.0f, (float) (1.0d / 3.0d));
        check("double to int truncates", 3, (int) 3.99d);
        check("nan is not equal to itself", true, Double.NaN != Double.NaN);
        check("nan detected", true, Double.isNaN(0.0d / 0.0d));
        check("positive infinity", true, Double.isInfinite(1.0d / 0.0d));
        check("negative zero", true,
                Double.doubleToLongBits(-0.0d) != Double.doubleToLongBits(0.0d));
        check("sqrt", 4.0d, Math.sqrt(16.0d));
        check("floor of negative", -4.0d, Math.floor(-3.5d));
        check("round half up", 4L, Math.round(3.5d));
    }

    private void strings() {
        check("concat", "abc", "a" + "b" + "c");
        check("dynamic concat", "n=42", "n=" + 42);
        check("equals not identity", true, "hello".equals(new StringBuilder("hello").toString()));
        check("hashCode is jdk-compatible", 99162322, "hello".hashCode());
        check("substring", "ell", "hello".substring(1, 4));
        check("split length", 3, "a,b,c".split(",").length);
        check("uppercase", "HELLO", "hello".toUpperCase(java.util.Locale.ROOT));
        check("indexOf missing", -1, "hello".indexOf('z'));
        check("string builder reverse", "olleh", new StringBuilder("hello").reverse().toString());
        check("format", "3.14", String.format(java.util.Locale.ROOT, "%.2f", 3.14159d));
        check("unicode length", 2, "√ø".length());
        check("compareTo ordering", true, "apple".compareTo("banana") < 0);
    }

    private void arrays() {
        int[] numbers = {5, 3, 1, 4, 2};
        java.util.Arrays.sort(numbers);
        check("sorted", "[1, 2, 3, 4, 5]", java.util.Arrays.toString(numbers));
        check("binary search", 2, java.util.Arrays.binarySearch(numbers, 3));

        int[][] grid = new int[3][4];
        grid[2][3] = 9;
        check("2d default", 0, grid[0][0]);
        check("2d assignment", 9, grid[2][3]);
        check("2d row length", 4, grid[1].length);

        String[] copy = java.util.Arrays.copyOf(new String[]{"a", "b"}, 3);
        check("copyOf pads with null", null, copy[2]);

        try {
            int ignored = numbers[beyond(numbers)];
            check("out of bounds throws", true, ignored == 0);
        } catch (ArrayIndexOutOfBoundsException expected) {
            check("out of bounds throws", true, true);
        }

        try {
            int ignored = numbers[9];
            check("constant out of bounds throws", true, ignored == 0);
        } catch (ArrayIndexOutOfBoundsException expected) {
            check("constant out of bounds throws", true, true);
        }
    }

    // an index the compiler cannot fold
    private int beyond(int[] array) {
        return array.length + checks + 4;
    }

    private void collections() {
        Map<String, Integer> counts = new HashMap<>();
        for (String word : "the quick brown the fox the".split(" ")) {
            counts.merge(word, 1, Integer::sum);
        }
        check("map merge", 3, counts.get("the"));
        check("map size", 4, counts.size());
        check("sorted map order", "[brown, fox, quick, the]",
                new TreeMap<>(counts).keySet().toString());

        List<Integer> list = new ArrayList<>(List.of(3, 1, 2));
        java.util.Collections.sort(list);
        check("list sort", "[1, 2, 3]", list.toString());
        check("list contains", true, list.contains(2));
        check("stream sum", 6, list.stream().mapToInt(Integer::intValue).sum());
        check("stream filter", "[2]", list.stream()
                .filter(n -> n % 2 == 0)
                .collect(java.util.stream.Collectors.toList())
                .toString());
        check("iterator walks all", 3, countByIterator(list));
        check("hash set dedupes", 2, new java.util.HashSet<>(List.of("a", "b", "a")).size());
    }

    private static int countByIterator(List<Integer> list) {
        int seen = 0;
        for (Integer ignored : list) {
            seen++;
        }
        return seen;
    }

    private void exceptions() {
        try {
            throw new IllegalStateException("thrown");
        } catch (IllegalStateException expected) {
            check("catch by type", "thrown", expected.getMessage());
        }

        try {
            try {
                throw new java.io.IOException("root");
            } catch (java.io.IOException cause) {
                throw new RuntimeException("wrapper", cause);
            }
        } catch (RuntimeException wrapper) {
            check("cause chaining", "root", wrapper.getCause().getMessage());
        }

        StringBuilder order = new StringBuilder();
        try {
            order.append("t");
            throw new RuntimeException("x");
        } catch (RuntimeException ignored) {
            order.append("c");
        } finally {
            order.append("f");
        }
        check("finally runs last", "tcf", order.toString());

        try {
            Object text = "not a number";
            Integer broken = (Integer) text;
            check("class cast throws", true, broken == null);
        } catch (ClassCastException expected) {
            check("class cast throws", true, true);
        }

        try {
            String nothing = null;
            check("npe is catchable", true, nothing.isEmpty());
        } catch (NullPointerException expected) {
            check("npe is catchable", true, true);
        }

        try {
            check("npe crosses a call boundary", true, lengthOf(null) < 0);
        } catch (NullPointerException expected) {
            check("npe crosses a call boundary", true, true);
        }
    }

    private static int lengthOf(String text) {
        return text.length();
    }

    private void entryPointArguments(String[] arguments) {
        Object raw = arguments;
        boolean typed;
        try {
            String[] cast = (String[]) raw;
            typed = cast.length == arguments.length;
        } catch (ClassCastException failure) {
            typed = false;
        }
        check("main receives a real String[]", true, typed);

        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                joined.append(',');
            }
            joined.append(arguments[i].trim());
        }
        Platforms.get().console().info("arguments: [" + joined + "]");
    }

    private void reflection() {
        check("class name", "dev.flagon.sandbox.RuntimeSelfTestProgram$Square",
                Square.class.getName());
        check("simple name", "Square", Square.class.getSimpleName());
        check("name off an instance", Square.class.getName(),
                new Square(1.0d).getClass().getName());
        check("isInstance", true, Square.class.isInstance(new Square(1.0d)));
        check("isInstance of another type", false, Square.class.isInstance("text"));
        check("isAssignableFrom", true, Shape.class.isAssignableFrom(Square.class));
        check("superclass", Base.class.getName(), Derived.class.getSuperclass().getName());
        check("interfaces are counted", 1, Square.class.getInterfaces().length);
        check("isInterface", true, Shape.class.isInterface());
        check("isArray", true, new int[0].getClass().isArray());
        check("cast keeps the object", 4.0d, Square.class.cast(new Square(2.0d)).area());
        check("array by reflection", 3, java.lang.reflect.Array.getLength(
                java.lang.reflect.Array.newInstance(String.class, 3)));

        try {
            check("forName finds a reachable class", "java.lang.String",
                    Class.forName("java.lang.String").getName());
        } catch (ClassNotFoundException missing) {
            check("forName finds a reachable class", "java.lang.String", "threw " + missing);
        }

        check("declared fields are stripped", 0, Square.class.getDeclaredFields().length);
        check("declared methods are stripped", 0, Square.class.getDeclaredMethods().length);
        check("constructors are stripped", 0, Square.class.getConstructors().length);

        try {
            Base made = Base.class.getDeclaredConstructor().newInstance();
            check("constructing by reflection is refused", true, made == null);
        } catch (ReflectiveOperationException expected) {
            check("constructing by reflection is refused", true, true);
        }

        try {
            Object proxied = java.lang.reflect.Proxy.newProxyInstance(
                    Shape.class.getClassLoader(), new Class<?>[] {Shape.class}, (p, m, a) -> 5.0d);
            check("proxying an interface nobody named is refused", true, proxied == null);
        } catch (SecurityException | IllegalArgumentException expected) {
            check("proxying an interface nobody named is refused", true, true);
        }

        Platforms.get().console().info("reflection: " + Square.class.getSimpleName() + " reports "
                + Square.class.getDeclaredMethods().length + " methods and "
                + Square.class.getDeclaredFields().length + " fields");
    }

    /**
     * whether a class can arrive after the compile, which is what a bytecode transformer needs
     */
    private void classDefinition() {
        try {
            Class.forName("dev.flagon.sandbox.NeverCompiled");
            check("a class the compile never saw is not found", true, false);
        } catch (ClassNotFoundException | RuntimeException expected) {
            check("a class the compile never saw is not found", true, true);
        }

        boolean canDefine;
        try {
            ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class,
                    int.class, int.class);
            canDefine = true;
        } catch (ReflectiveOperationException | RuntimeException absent) {
            canDefine = false;
        }
        check("no class loader defines a class from bytes", false, canDefine);

        Platforms.get().console().info("selftest: bytecode arriving at runtime is "
                + (canDefine ? "possible" : "impossible") + " here");
    }

    /**
     * the other half: what a class opted into member reflection can do
     */
    private void optedInReflection() {
        check("annotated fields survive", 2, Reflected.class.getDeclaredFields().length);
        check("annotated constructors survive", 2,
                Reflected.class.getDeclaredConstructors().length);

        try {
            Object made = Reflected.class.getDeclaredConstructor(String.class, int.class)
                    .newInstance("made", 3);
            check("constructing by reflection", "made", ((Reflected) made).label());
            check("invoking by reflection", 6,
                    Reflected.class.getDeclaredMethod("twice").invoke(made));

            java.lang.reflect.Field label = Reflected.class.getDeclaredField("label");
            label.setAccessible(true);
            check("reading a private field", "made", label.get(made));
            label.set(made, "renamed");
            check("writing a private field", "renamed", ((Reflected) made).label());
        } catch (ReflectiveOperationException failed) {
            check("an opted-in class reflects", "no failure", "threw " + failed);
        }

        try {
            Class<?> named = Class.forName("dev.flagon.sandbox.ReflectedByPattern");
            Object made = named.getDeclaredConstructor().newInstance();
            check("a class named by the build is found", "dev.flagon.sandbox.ReflectedByPattern",
                    named.getName());
            check("invoking on it", "named by pattern",
                    named.getDeclaredMethod("greet").invoke(made));
        } catch (ReflectiveOperationException failed) {
            check("a class named by the build reflects", "no failure", "threw " + failed);
        }

        try {
            Proxied stood = (Proxied) java.lang.reflect.Proxy.newProxyInstance(
                    Proxied.class.getClassLoader(), new Class<?>[] {Proxied.class},
                    (proxy, method, arguments) -> method.getName() + " " + arguments[0]);
            check("proxying an interface the build named", "greet world", stood.greet("world"));
            Platforms.get().console().info("reflection: Proxied stands in, saying "
                    + stood.greet("world"));
        } catch (RuntimeException failed) {
            check("proxying an interface the build named", "greet world", "threw " + failed);
        }

        try {
            Class<?> listed = Class.forName("dev.flagon.sandbox.ConstructedByName");
            Object made = listed.getDeclaredConstructor(String.class).newInstance("listed");
            check("a class listed for the compiler is constructed", "listed",
                    ((ConstructedByName) made).label());
            check("constructors are all it keeps", 0, listed.getDeclaredMethods().length);
            Platforms.get().console().info("reflection: ConstructedByName reports "
                    + listed.getDeclaredConstructors().length + " constructors and "
                    + listed.getDeclaredMethods().length + " methods");
        } catch (ReflectiveOperationException failed) {
            check("a class listed for the compiler is constructed", "listed", "threw " + failed);
        }

        try {
            check("a converted type keeps valueOf(String)", 42,
                    Integer.class.getDeclaredMethod("valueOf", String.class).invoke(null, "42"));
            check("a converted type keeps its (String) constructor", 7,
                    Integer.class.getConstructor(String.class).newInstance("7"));
            check("a converted type keeps nothing else", false,
                    declares(Integer.class, "divideUnsigned"));
        } catch (ReflectiveOperationException failed) {
            check("a converted type reflects", "no failure", "threw " + failed);
        }

        Platforms.get().console().info("reflection: " + Reflected.class.getSimpleName()
                + " reports "
                + Reflected.class.getDeclaredMethods().length + " methods and "
                + Reflected.class.getDeclaredFields().length + " fields");
    }

    private interface Shape {
        double area();

        default String label() {
            return "shape@" + Math.round(area());
        }
    }

    private record Square(double side) implements Shape {
        @Override
        public double area() {
            return side * side;
        }
    }

    private static class Base {
        String who() {
            return "base";
        }
    }

    private static final class Derived extends Base {
        @Override
        String who() {
            return "derived/" + super.who();
        }
    }

    private void polymorphism() {
        Shape shape = new Square(3.0d);
        check("interface dispatch", 9.0d, shape.area());
        check("default method", "shape@9", shape.label());
        check("record equality", true, new Square(3.0d).equals(new Square(3.0d)));
        check("virtual dispatch", "derived/base", new Derived().who());
        check("instanceof", true, shape instanceof Square);
        check("upcast dispatch", "derived/base", ((Base) new Derived()).who());
    }

    private void lambdasAndClosures() {
        AtomicInteger counter = new AtomicInteger();
        Runnable increment = counter::incrementAndGet;
        increment.run();
        increment.run();
        check("method reference closure", 2, counter.get());

        int captured = 10;
        java.util.function.IntUnaryOperator addCaptured = n -> n + captured;
        check("captured local", 15, addCaptured.applyAsInt(5));

        java.util.function.Function<Integer, java.util.function.Function<Integer,
                Integer>> curried =
                a -> b -> a * b;
        check("nested lambda", 12, curried.apply(3).apply(4));
    }

    private void clockBehaviour() {
        Clock clock = Platforms.get().clock();
        check("wall clock is post-2020", true, clock.currentTimeMillis() > 1_577_836_800_000L);

        long previous = clock.nanoTime();
        boolean monotonic = true;
        for (int i = 0; i < 2000; i++) {
            long now = clock.nanoTime();
            if (now < previous) {
                monotonic = false;
                break;
            }
            previous = now;
        }
        check("nanoTime never decreases", true, monotonic);
    }

    /** nio buffers are how the game hands vertex and pixel data to gl, so they run here too. */
    private void nioBuffers() {
        ByteBuffer bytes = BufferUtils.createByteBuffer(16);
        check("byte buffer capacity", 16, bytes.capacity());
        check("buffers are native order", ByteOrder.nativeOrder(), bytes.order());

        FloatBuffer floats = BufferUtils.createFloatBuffer(4);
        floats.put(0, 1.5f).put(1, -2.25f);
        check("float buffer round trip", 1.5f, floats.get(0));
        check("float buffer holds negatives", -2.25f, floats.get(1));

        IntBuffer view = bytes.asIntBuffer();
        view.put(0, 0x0A0B0C0D);
        check("typed views alias the same bytes", 0x0A0B0C0D, bytes.asIntBuffer().get(0));

        IntBuffer zeroed = BufferUtils.createIntBuffer(2);
        zeroed.put(0, 5).put(1, 5);
        zeroed.position(1);
        BufferUtils.zeroBuffer(zeroed);
        check("zeroBuffer skips consumed elements", 5, zeroed.get(0));
        check("zeroBuffer clears the rest", 0, zeroed.get(1));

        check("ascii round trip", "flagon",
                MemoryUtil.decodeASCII(MemoryUtil.encodeASCII("flagon")));
        check("utf8 round trip", "héllo ☃",
                MemoryUtil.decodeUTF8(MemoryUtil.encodeUTF8("héllo ☃")));
        check("utf8 encodes bytes not chars", 3, MemoryUtil.encodeUTF8("☃").remaining());
        check("utf16 round trip", "héllo ☃",
                MemoryUtil.decodeUTF16(MemoryUtil.encodeUTF16("héllo ☃")));

        boolean refused = false;
        try {
            MemoryUtil.getAddress(bytes);
        } catch (UnsupportedCapabilityException expected) {
            refused = true;
        }
        check("buffer addresses are refused", true, refused);
    }

    private static boolean declares(Class<?> type, String name) {
        for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void check(String name, Object expected, Object actual) {
        checks++;
        boolean equal = expected == null ? actual == null : expected.equals(actual);
        if (!equal) {
            failures.add(name + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
