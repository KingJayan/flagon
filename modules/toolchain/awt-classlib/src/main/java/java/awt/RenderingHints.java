package java.awt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RenderingHints implements Map<Object, Object>, Cloneable {

    public abstract static class Key {

        private final int key;

        protected Key(int privateKey) {
            this.key = privateKey;
        }

        public abstract boolean isCompatibleValue(Object value);

        protected final int intKey() {
            return key;
        }

        @Override
        public final int hashCode() {
            return super.hashCode();
        }

        @Override
        public final boolean equals(Object other) {
            return this == other;
        }
    }

    private static final class Named extends Key {

        private final String name;

        Named(int key, String name) {
            super(key);
            this.name = name;
        }

        @Override
        public boolean isCompatibleValue(Object value) {
            return value instanceof Value hint && hint.key == this;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class Contrast extends Key {

        Contrast(int key) {
            super(key);
        }

        @Override
        public boolean isCompatibleValue(Object value) {
            return value instanceof Integer contrast && contrast >= 100 && contrast <= 250;
        }

        @Override
        public String toString() {
            return "Text-specific LCD contrast key";
        }
    }

    private static final class Value {

        private final Key key;
        private final String name;

        Value(Key key, String name) {
            this.key = key;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final Key KEY_ANTIALIASING = new Named(0, "Global antialiasing enable key");
    public static final Object VALUE_ANTIALIAS_ON =
            new Value(KEY_ANTIALIASING, "Antialiased rendering mode");
    public static final Object VALUE_ANTIALIAS_OFF =
            new Value(KEY_ANTIALIASING, "Nonantialiased rendering mode");
    public static final Object VALUE_ANTIALIAS_DEFAULT =
            new Value(KEY_ANTIALIASING, "Default antialiasing rendering mode");

    public static final Key KEY_RENDERING = new Named(1, "Global rendering quality key");
    public static final Object VALUE_RENDER_SPEED =
            new Value(KEY_RENDERING, "Fastest rendering methods");
    public static final Object VALUE_RENDER_QUALITY =
            new Value(KEY_RENDERING, "Highest quality rendering methods");
    public static final Object VALUE_RENDER_DEFAULT =
            new Value(KEY_RENDERING, "Default rendering methods");

    public static final Key KEY_DITHERING = new Named(2, "Dithering quality key");
    public static final Object VALUE_DITHER_DISABLE =
            new Value(KEY_DITHERING, "Nondithered rendering mode");
    public static final Object VALUE_DITHER_ENABLE =
            new Value(KEY_DITHERING, "Dithered rendering mode");
    public static final Object VALUE_DITHER_DEFAULT =
            new Value(KEY_DITHERING, "Default dithering mode");

    public static final Key KEY_TEXT_ANTIALIASING =
            new Named(3, "Text-specific antialiasing enable key");
    public static final Object VALUE_TEXT_ANTIALIAS_ON =
            new Value(KEY_TEXT_ANTIALIASING, "Antialiased text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_OFF =
            new Value(KEY_TEXT_ANTIALIASING, "Nonantialiased text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_DEFAULT =
            new Value(KEY_TEXT_ANTIALIASING, "Default antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_GASP =
            new Value(KEY_TEXT_ANTIALIASING, "gasp antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_LCD_HRGB =
            new Value(KEY_TEXT_ANTIALIASING, "LCD HRGB antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_LCD_HBGR =
            new Value(KEY_TEXT_ANTIALIASING, "LCD HBGR antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_LCD_VRGB =
            new Value(KEY_TEXT_ANTIALIASING, "LCD VRGB antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_LCD_VBGR =
            new Value(KEY_TEXT_ANTIALIASING, "LCD VBGR antialiasing text mode");

    public static final Key KEY_TEXT_LCD_CONTRAST = new Contrast(4);

    public static final Key KEY_FRACTIONALMETRICS =
            new Named(5, "Fractional metrics enable key");
    public static final Object VALUE_FRACTIONALMETRICS_OFF =
            new Value(KEY_FRACTIONALMETRICS, "Integer text metrics mode");
    public static final Object VALUE_FRACTIONALMETRICS_ON =
            new Value(KEY_FRACTIONALMETRICS, "Fractional text metrics mode");
    public static final Object VALUE_FRACTIONALMETRICS_DEFAULT =
            new Value(KEY_FRACTIONALMETRICS, "Default fractional text metrics mode");

    public static final Key KEY_INTERPOLATION = new Named(6, "Image interpolation method key");
    public static final Object VALUE_INTERPOLATION_NEAREST_NEIGHBOR =
            new Value(KEY_INTERPOLATION, "Nearest Neighbor image interpolation mode");
    public static final Object VALUE_INTERPOLATION_BILINEAR =
            new Value(KEY_INTERPOLATION, "Bilinear image interpolation mode");
    public static final Object VALUE_INTERPOLATION_BICUBIC =
            new Value(KEY_INTERPOLATION, "Bicubic image interpolation mode");

    public static final Key KEY_ALPHA_INTERPOLATION =
            new Named(7, "Alpha blending interpolation method key");
    public static final Object VALUE_ALPHA_INTERPOLATION_SPEED =
            new Value(KEY_ALPHA_INTERPOLATION, "Fastest alpha blending methods");
    public static final Object VALUE_ALPHA_INTERPOLATION_QUALITY =
            new Value(KEY_ALPHA_INTERPOLATION, "Highest quality alpha blending methods");
    public static final Object VALUE_ALPHA_INTERPOLATION_DEFAULT =
            new Value(KEY_ALPHA_INTERPOLATION, "Default alpha blending methods");

    public static final Key KEY_COLOR_RENDERING = new Named(8, "Color rendering quality key");
    public static final Object VALUE_COLOR_RENDER_SPEED =
            new Value(KEY_COLOR_RENDERING, "Fastest color rendering mode");
    public static final Object VALUE_COLOR_RENDER_QUALITY =
            new Value(KEY_COLOR_RENDERING, "Highest quality color rendering mode");
    public static final Object VALUE_COLOR_RENDER_DEFAULT =
            new Value(KEY_COLOR_RENDERING, "Default color rendering mode");

    public static final Key KEY_STROKE_CONTROL =
            new Named(9, "Stroke normalization control key");
    public static final Object VALUE_STROKE_DEFAULT =
            new Value(KEY_STROKE_CONTROL, "Default stroke normalization");
    public static final Object VALUE_STROKE_NORMALIZE =
            new Value(KEY_STROKE_CONTROL, "Normalize strokes for consistent rendering");
    public static final Object VALUE_STROKE_PURE =
            new Value(KEY_STROKE_CONTROL, "Pure stroke conversion for accurate paths");

    private HashMap<Object, Object> hints = new HashMap<>(7);

    public RenderingHints(Map<Key, ?> initial) {
        if (initial != null) {
            putAll(initial);
        }
    }

    public RenderingHints(Key key, Object value) {
        put(key, value);
    }

    @Override
    public int size() {
        return hints.size();
    }

    @Override
    public boolean isEmpty() {
        return hints.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return hints.containsKey((Key) key);
    }

    @Override
    public boolean containsValue(Object value) {
        return hints.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return hints.get((Key) key);
    }

    @Override
    public Object put(Object key, Object value) {
        if (!((Key) key).isCompatibleValue(value)) {
            throw new IllegalArgumentException(value + " incompatible with " + key);
        }
        return hints.put(key, value);
    }

    public void add(RenderingHints other) {
        hints.putAll(other.hints);
    }

    @Override
    public void clear() {
        hints.clear();
    }

    @Override
    public Object remove(Object key) {
        return hints.remove((Key) key);
    }

    @Override
    public void putAll(Map<?, ?> other) {
        if (other instanceof RenderingHints given) {
            hints.putAll(given.hints);
            return;
        }
        for (Map.Entry<?, ?> entry : other.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Set<Object> keySet() {
        return hints.keySet();
    }

    @Override
    public Collection<Object> values() {
        return hints.values();
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return hints.entrySet();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RenderingHints given) {
            return hints.equals(given.hints);
        }
        return other instanceof Map && hints.equals(other);
    }

    @Override
    public int hashCode() {
        return hints.hashCode();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object clone() {
        try {
            RenderingHints copy = (RenderingHints) super.clone();
            copy.hints = (HashMap<Object, Object>) hints.clone();
            return copy;
        } catch (CloneNotSupportedException impossible) {
            throw new InternalError(impossible);
        }
    }

    @Override
    public String toString() {
        return hints.toString();
    }
}
