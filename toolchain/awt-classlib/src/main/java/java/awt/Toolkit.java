package java.awt;

import java.awt.datatransfer.Clipboard;

public abstract class Toolkit {

    private static Toolkit only;

    protected Toolkit() {
    }

    public static synchronized Toolkit getDefaultToolkit() {
        if (only == null) {
            only = new BrowserToolkit();
        }
        return only;
    }

    public abstract Clipboard getSystemClipboard();

    @Deprecated
    public FontMetrics getFontMetrics(Font font) {
        return dev.flagon.awt.Text.metrics(font);
    }

    public void beep() {
    }

    private static final class BrowserToolkit extends Toolkit {

        private final Clipboard clipboard = new Clipboard("system");

        @Override
        public Clipboard getSystemClipboard() {
            return clipboard;
        }
    }
}
