package java.awt;

public class Color implements Paint {

    public static final Color white = new Color(255, 255, 255);
    public static final Color WHITE = white;
    public static final Color lightGray = new Color(192, 192, 192);
    public static final Color LIGHT_GRAY = lightGray;
    public static final Color gray = new Color(128, 128, 128);
    public static final Color GRAY = gray;
    public static final Color darkGray = new Color(64, 64, 64);
    public static final Color DARK_GRAY = darkGray;
    public static final Color black = new Color(0, 0, 0);
    public static final Color BLACK = black;
    public static final Color red = new Color(255, 0, 0);
    public static final Color RED = red;
    public static final Color pink = new Color(255, 175, 175);
    public static final Color PINK = pink;
    public static final Color orange = new Color(255, 200, 0);
    public static final Color ORANGE = orange;
    public static final Color yellow = new Color(255, 255, 0);
    public static final Color YELLOW = yellow;
    public static final Color green = new Color(0, 255, 0);
    public static final Color GREEN = green;
    public static final Color magenta = new Color(255, 0, 255);
    public static final Color MAGENTA = magenta;
    public static final Color cyan = new Color(0, 255, 255);
    public static final Color CYAN = cyan;
    public static final Color blue = new Color(0, 0, 255);
    public static final Color BLUE = blue;

    private static final double FACTOR = 0.7;

    private final int value;

    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public Color(int r, int g, int b, int a) {
        requireRange(r, g, b, a);
        value = (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
    }

    public Color(int rgb) {
        value = 0xFF000000 | rgb;
    }

    public Color(int argb, boolean hasAlpha) {
        value = hasAlpha ? argb : 0xFF000000 | argb;
    }

    public Color(float r, float g, float b) {
        this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5));
    }

    public Color(float r, float g, float b, float a) {
        this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5),
                (int) (a * 255 + 0.5));
    }

    private static void requireRange(int r, int g, int b, int a) {
        String wrong = (a < 0 || a > 255 ? " Alpha" : "") + (r < 0 || r > 255 ? " Red" : "")
                + (g < 0 || g > 255 ? " Green" : "") + (b < 0 || b > 255 ? " Blue" : "");
        if (!wrong.isEmpty()) {
            throw new IllegalArgumentException("Color parameter outside of expected range:"
                    + wrong);
        }
    }

    public int getRed() {
        return (value >> 16) & 0xFF;
    }

    public int getGreen() {
        return (value >> 8) & 0xFF;
    }

    public int getBlue() {
        return value & 0xFF;
    }

    public int getAlpha() {
        return (value >>> 24) & 0xFF;
    }

    public int getRGB() {
        return value;
    }

    public Color brighter() {
        int r = getRed();
        int g = getGreen();
        int b = getBlue();
        int least = (int) (1.0 / (1.0 - FACTOR));
        if (r == 0 && g == 0 && b == 0) {
            return new Color(least, least, least, getAlpha());
        }
        return new Color(brighter(r, least), brighter(g, least), brighter(b, least), getAlpha());
    }

    private static int brighter(int channel, int least) {
        int lifted = channel > 0 && channel < least ? least : channel;
        return Math.min((int) (lifted / FACTOR), 255);
    }

    public Color darker() {
        return new Color(Math.max((int) (getRed() * FACTOR), 0),
                Math.max((int) (getGreen() * FACTOR), 0),
                Math.max((int) (getBlue() * FACTOR), 0), getAlpha());
    }

    public static Color decode(String text) {
        int rgb = Integer.decode(text);
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0;
        int g = 0;
        int b = 0;
        if (saturation == 0) {
            r = (int) (brightness * 255.0f + 0.5f);
            g = r;
            b = r;
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - saturation * (1.0f - f));
            float[] rgb = switch ((int) h) {
                case 0 -> new float[] {brightness, t, p};
                case 1 -> new float[] {q, brightness, p};
                case 2 -> new float[] {p, brightness, t};
                case 3 -> new float[] {p, q, brightness};
                case 4 -> new float[] {t, p, brightness};
                case 5 -> new float[] {brightness, p, q};
                default -> new float[3];
            };
            r = (int) (rgb[0] * 255.0f + 0.5f);
            g = (int) (rgb[1] * 255.0f + 0.5f);
            b = (int) (rgb[2] * 255.0f + 0.5f);
        }
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static float[] RGBtoHSB(int r, int g, int b, float[] into) {
        float[] hsb = into == null ? new float[3] : into;
        int most = Math.max(r, Math.max(g, b));
        int least = Math.min(r, Math.min(g, b));
        float brightness = most / 255.0f;
        float saturation = most != 0 ? (float) (most - least) / most : 0;
        float hue = 0;
        if (saturation != 0) {
            float range = most - least;
            float redc = (most - r) / range;
            float greenc = (most - g) / range;
            float bluec = (most - b) / range;
            if (r == most) {
                hue = bluec - greenc;
            } else if (g == most) {
                hue = 2.0f + redc - bluec;
            } else {
                hue = 4.0f + greenc - redc;
            }
            hue = hue / 6.0f;
            if (hue < 0) {
                hue = hue + 1.0f;
            }
        }
        hsb[0] = hue;
        hsb[1] = saturation;
        hsb[2] = brightness;
        return hsb;
    }

    public static Color getHSBColor(float hue, float saturation, float brightness) {
        return new Color(HSBtoRGB(hue, saturation, brightness));
    }

    public float[] getRGBComponents(float[] into) {
        float[] components = into == null ? new float[4] : into;
        components[0] = getRed() / 255f;
        components[1] = getGreen() / 255f;
        components[2] = getBlue() / 255f;
        components[3] = getAlpha() / 255f;
        return components;
    }

    public float[] getRGBColorComponents(float[] into) {
        float[] components = into == null ? new float[3] : into;
        components[0] = getRed() / 255f;
        components[1] = getGreen() / 255f;
        components[2] = getBlue() / 255f;
        return components;
    }

    public float[] getComponents(float[] into) {
        return getRGBComponents(into);
    }

    public float[] getColorComponents(float[] into) {
        return getRGBColorComponents(into);
    }

    @Override
    public int getTransparency() {
        int alpha = getAlpha();
        return alpha == 0xFF ? OPAQUE : alpha == 0 ? BITMASK : TRANSLUCENT;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Color color && color.getRGB() == value;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue()
                + "]";
    }
}
