package java.awt;

import java.awt.geom.Rectangle2D;
import java.nio.charset.StandardCharsets;

public abstract class FontMetrics {

    protected Font font;

    protected FontMetrics(Font font) {
        this.font = font;
    }

    public Font getFont() {
        return font;
    }

    public int getLeading() {
        return 0;
    }

    public int getAscent() {
        return font.getSize();
    }

    public int getDescent() {
        return 0;
    }

    public int getHeight() {
        return getLeading() + getAscent() + getDescent();
    }

    public int getMaxAscent() {
        return getAscent();
    }

    public int getMaxDescent() {
        return getDescent();
    }

    public int getMaxAdvance() {
        return -1;
    }

    public int charWidth(int codePoint) {
        return stringWidth(new String(Character.toChars(codePoint)));
    }

    public int charWidth(char character) {
        return stringWidth(String.valueOf(character));
    }

    public abstract int stringWidth(String text);

    public int charsWidth(char[] text, int offset, int length) {
        return stringWidth(new String(text, offset, length));
    }

    public int bytesWidth(byte[] text, int offset, int length) {
        return stringWidth(new String(text, offset, length, StandardCharsets.ISO_8859_1));
    }

    public int[] getWidths() {
        int[] widths = new int[256];
        for (char character = 0; character < 256; character++) {
            widths[character] = charWidth(character);
        }
        return widths;
    }

    public Rectangle2D getStringBounds(String text, Graphics context) {
        return new Rectangle2D.Float(0, -getAscent(), stringWidth(text), getHeight());
    }

    @Override
    public String toString() {
        return getClass().getName() + "[font=" + getFont() + "ascent=" + getAscent()
                + ", descent=" + getDescent() + ", height=" + getHeight() + "]";
    }
}
