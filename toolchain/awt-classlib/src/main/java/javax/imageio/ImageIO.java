package javax.imageio;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public final class ImageIO {

    private ImageIO() {
    }

    public static BufferedImage read(InputStream from) throws IOException {
        if (from == null) {
            throw new IllegalArgumentException("there is no stream to read an image from");
        }
        return Png.read(from);
    }

    public static BufferedImage read(File file) throws IOException {
        try (InputStream from = new FileInputStream(file)) {
            return Png.read(from);
        }
    }

    public static boolean write(RenderedImage image, String format, OutputStream to)
            throws IOException {
        if (!isPng(format)) {
            return false;
        }
        Png.write(asBuffered(image), to);
        return true;
    }

    public static boolean write(RenderedImage image, String format, File file)
            throws IOException {
        if (!isPng(format)) {
            return false;
        }
        try (OutputStream to = new FileOutputStream(file)) {
            Png.write(asBuffered(image), to);
        }
        return true;
    }

    public static void setUseCache(boolean useCache) {
    }

    public static boolean getUseCache() {
        return false;
    }

    public static void setCacheDirectory(File directory) {
    }

    public static String[] getReaderFormatNames() {
        return new String[] {"png", "PNG"};
    }

    public static String[] getWriterFormatNames() {
        return getReaderFormatNames();
    }

    private static boolean isPng(String format) {
        return format != null && "png".equals(format.toLowerCase(Locale.ROOT));
    }

    private static BufferedImage asBuffered(RenderedImage image) {
        if (image instanceof BufferedImage buffered) {
            return buffered;
        }
        throw new UnsupportedOperationException(
                "ImageIO writes a BufferedImage here; there is no other kind of image in a "
                        + "bundle");
    }
}
