package javax.imageio;

import dev.flagon.platform.Platforms;
import dev.flagon.platform.graphics.Drawing;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Locale;

/** png in java, which is exact; jpeg, gif, bmp, webp and interlaced png by the browser */
public final class ImageIO {

    private ImageIO() {
    }

    public static BufferedImage read(InputStream from) throws IOException {
        if (from == null) {
            throw new IllegalArgumentException("input == null!");
        }
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        from.transferTo(all);
        return read(all.toByteArray());
    }

    public static BufferedImage read(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("input == null!");
        }
        if (!file.canRead()) {
            throw new IIOException("Can't read input file!");
        }
        try (InputStream from = new FileInputStream(file)) {
            return read(from);
        }
    }

    public static BufferedImage read(URL url) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("input == null!");
        }
        try (InputStream from = url.openStream()) {
            return read(from);
        } catch (IOException unread) {
            throw new IIOException("Can't get input stream from URL!", unread);
        }
    }

    private static BufferedImage read(byte[] file) throws IOException {
        String format = formatOf(file);
        if (format == null) {
            return null;
        }
        if (format.equals("png") && !Png.interlaced(file)) {
            return Png.read(new ByteArrayInputStream(file));
        }
        Drawing.Decoded decoded = Platforms.get().drawing().decode(file);
        int type = format.equals("jpeg") ? BufferedImage.TYPE_3BYTE_BGR
                : BufferedImage.TYPE_INT_ARGB;
        BufferedImage image = new BufferedImage(decoded.width(), decoded.height(), type);
        image.setRGB(0, 0, decoded.width(), decoded.height(), decoded.argb(), 0,
                decoded.width());
        return image;
    }

    /** @return the format the file's first bytes say it is, or null for one not read here */
    private static String formatOf(byte[] file) {
        if (starts(file, 0x89, 'P', 'N', 'G')) {
            return "png";
        }
        if (starts(file, 0xFF, 0xD8, 0xFF)) {
            return "jpeg";
        }
        if (starts(file, 'G', 'I', 'F', '8')) {
            return "gif";
        }
        if (starts(file, 'B', 'M')) {
            return "bmp";
        }
        if (starts(file, 'R', 'I', 'F', 'F') && file.length >= 12 && file[8] == 'W'
                && file[9] == 'E' && file[10] == 'B' && file[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private static boolean starts(byte[] file, int... magic) {
        if (file.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if ((file[i] & 0xFF) != magic[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean write(RenderedImage image, String format, OutputStream to)
            throws IOException {
        if (image == null || format == null || to == null) {
            throw new IllegalArgumentException("an argument is null");
        }
        if (!isPng(format)) {
            return false;
        }
        Png.write(asBuffered(image), to);
        return true;
    }

    public static boolean write(RenderedImage image, String format, File file)
            throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("output == null!");
        }
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
        return new String[] {"png", "PNG", "jpg", "JPG", "jpeg", "JPEG", "gif", "GIF", "bmp",
            "BMP", "webp", "WEBP"};
    }

    public static String[] getWriterFormatNames() {
        return new String[] {"png", "PNG"};
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
