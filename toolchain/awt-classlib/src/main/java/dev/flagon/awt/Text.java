package dev.flagon.awt;

import dev.flagon.platform.Platforms;
import dev.flagon.platform.graphics.Canvas2D;

import java.awt.Font;
import java.awt.FontMetrics;

/** text through the page's canvas: java fonts as css fonts, measured where they are drawn */
public final class Text {

    private static Canvas2D measuring;

    private Text() {
    }

    /** @return the css font a canvas draws this java font with */
    public static String css(Font font) {
        String family = switch (font.getName()) {
            case Font.DIALOG, Font.SANS_SERIF, "Default" -> "sans-serif";
            case Font.SERIF -> "serif";
            case Font.MONOSPACED, Font.DIALOG_INPUT -> "monospace";
            default -> "\"" + font.getName().replace("\"", "") + "\", sans-serif";
        };
        return (font.isItalic() ? "italic " : "") + (font.isBold() ? "bold " : "")
                + font.getSize2D() + "px " + family;
    }

    /** @return width, ascent and descent of the text in this font */
    public static double[] measure(Font font, String text) {
        if (measuring == null) {
            measuring = Platforms.get().drawing().canvas(1, 1);
        }
        measuring.font(css(font));
        return measuring.measure(text);
    }

    public static FontMetrics metrics(Font font) {
        return new Metrics(font);
    }

    private static final class Metrics extends FontMetrics {

        private static final double ROUND_UP = 0.95;

        private final int ascent;
        private final int descent;

        Metrics(Font font) {
            super(font);
            double[] measured = measure(font, "");
            this.ascent = (int) (ROUND_UP + measured[1]);
            this.descent = (int) (ROUND_UP + measured[2]);
        }

        @Override
        public int getAscent() {
            return ascent;
        }

        @Override
        public int getDescent() {
            return descent;
        }

        @Override
        public int stringWidth(String text) {
            return (int) (0.5 + measure(font, text)[0]);
        }
    }
}
