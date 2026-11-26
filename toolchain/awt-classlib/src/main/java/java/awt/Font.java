package java.awt;

import java.util.Locale;

public class Font {

    public static final String DIALOG = "Dialog";
    public static final String DIALOG_INPUT = "DialogInput";
    public static final String SANS_SERIF = "SansSerif";
    public static final String SERIF = "Serif";
    public static final String MONOSPACED = "Monospaced";
    public static final int PLAIN = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;

    protected String name;
    protected int style;
    protected int size;
    protected float pointSize;

    public Font(String name, int style, int size) {
        this(name, style, (float) size);
        this.size = size;
    }

    private Font(String name, int style, float size) {
        this.name = name == null ? "Default" : name;
        this.style = (style & ~(BOLD | ITALIC)) == 0 ? style : PLAIN;
        this.size = (int) (size + 0.5);
        this.pointSize = size;
    }

    protected Font(Font font) {
        this(font.name, font.style, font.pointSize);
        this.size = font.size;
    }

    public static Font decode(String text) {
        if (text == null) {
            return new Font(DIALOG, PLAIN, 12);
        }
        char separator = text.lastIndexOf('-') > text.lastIndexOf(' ') ? '-' : ' ';
        String[] parts = text.split(java.util.regex.Pattern.quote(String.valueOf(separator)));
        String name = text;
        int style = PLAIN;
        int size = 12;
        int used = parts.length;
        if (used > 1) {
            try {
                size = Integer.parseInt(parts[used - 1]);
                used--;
            } catch (NumberFormatException notSize) {
                size = 12;
            }
        }
        if (used > 1) {
            int read = styleOf(parts[used - 1]);
            if (read >= 0) {
                style = read;
                used--;
            }
        }
        if (used < parts.length) {
            name = String.join(String.valueOf(separator), java.util.Arrays.copyOf(parts, used));
        }
        return new Font(name, style, size);
    }

    private static int styleOf(String word) {
        return switch (word.toLowerCase(Locale.ROOT)) {
            case "plain" -> PLAIN;
            case "bold" -> BOLD;
            case "italic" -> ITALIC;
            case "bolditalic" -> BOLD | ITALIC;
            default -> -1;
        };
    }

    public String getName() {
        return name;
    }

    public String getFontName() {
        return name;
    }

    public String getFamily() {
        return name;
    }

    public int getStyle() {
        return style;
    }

    public int getSize() {
        return size;
    }

    public float getSize2D() {
        return pointSize;
    }

    public boolean isPlain() {
        return style == PLAIN;
    }

    public boolean isBold() {
        return (style & BOLD) != 0;
    }

    public boolean isItalic() {
        return (style & ITALIC) != 0;
    }

    public Font deriveFont(float size) {
        return new Font(name, style, size);
    }

    public Font deriveFont(int style) {
        Font derived = new Font(name, style, pointSize);
        derived.size = size;
        return derived;
    }

    public Font deriveFont(int style, float size) {
        return new Font(name, style, size);
    }

    public boolean canDisplay(char character) {
        return true;
    }

    public boolean canDisplay(int codePoint) {
        return true;
    }

    public int canDisplayUpTo(String text) {
        return -1;
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ style ^ size;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof Font font && font.style == style
                && font.size == size && font.pointSize == pointSize && font.name.equals(name);
    }

    @Override
    public String toString() {
        String written = isBold() ? isItalic() ? "bolditalic" : "bold"
                : isItalic() ? "italic" : "plain";
        return getClass().getName() + "[family=" + getFamily() + ",name=" + name + ",style="
                + written + ",size=" + size + "]";
    }
}
