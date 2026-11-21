package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

public final class ImageProgram implements Program {

    @Override
    public String id() {
        return "image";
    }

    @Override
    public String description() {
        return "writes a png, reads it back, and copies a line of text";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        ConsoleSink console = Platforms.get().console();

        BufferedImage drawn = new BufferedImage(4, 2, BufferedImage.TYPE_INT_ARGB);
        drawn.setRGB(0, 0, 0xFF3366CC);
        drawn.setRGB(3, 1, 0x80FFFFFF);
        console.info("image: a pixel reads back as 0x"
                + Integer.toHexString(drawn.getRGB(0, 0)).toUpperCase(java.util.Locale.ROOT));

        int[] raw = ((DataBufferInt) drawn.getRaster().getDataBuffer()).getData();
        console.info("image: the raster holds " + raw.length + " pixels");

        ByteArrayOutputStream written = new ByteArrayOutputStream();
        boolean wrote = ImageIO.write(drawn, "png", written);
        console.info("image: png written " + wrote + " in " + written.size() + " bytes");

        BufferedImage read = ImageIO.read(new ByteArrayInputStream(written.toByteArray()));
        console.info("image: png read back " + read.getWidth() + "x" + read.getHeight()
                + " same pixels " + samePixels(drawn, read));

        console.info("image: jpeg is refused " + !ImageIO.write(drawn, "jpeg", written));

        BufferedImage atlas = new BufferedImage(8, 4, BufferedImage.TYPE_INT_ARGB);
        Graphics into = atlas.getGraphics();
        into.drawImage(drawn, 0, 0, 8, 4, 0, 0, 4, 2, null);
        into.dispose();
        console.info("image: a scaled copy carries the corner 0x"
                + Integer.toHexString(atlas.getRGB(1, 1)).toUpperCase(java.util.Locale.ROOT));

        readWritten(console, "greyscale", grey());
        readWritten(console, "palette", palette());
        // minecraft's unicode font pages are 1-bit palettes
        readWritten(console, "1-bit palette", png(4, 2, 1, 3,
                new byte[] {0, (byte) 0b1010_0000, 0, 0}, new byte[] {0, 0, 0, (byte) 255,
                    (byte) 255, (byte) 255}));
        readWritten(console, "4-bit greyscale", png(4, 2, 4, 0,
                new byte[] {0, (byte) 0xF0, 0, 0, 0, 0x08}, null));

        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection("copied in the page"), null);
        Object back = Toolkit.getDefaultToolkit().getSystemClipboard()
                .getContents(null).getTransferData(DataFlavor.stringFlavor);
        console.info("image: the clipboard hands back " + back);
    }

    private static void readWritten(ConsoleSink console, String what, byte[] file)
            throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(file));
        console.info("image: " + what + " reads " + image.getWidth() + "x" + image.getHeight()
                + " first 0x" + hex(image.getRGB(0, 0))
                + " last 0x" + hex(image.getRGB(image.getWidth() - 1, image.getHeight() - 1)));
    }

    private static byte[] grey() throws java.io.IOException {
        byte[] rows = {0, 10, 20, 30, 40, 1, 50, 10, 10, 10};
        return png(4, 2, 8, 0, rows, null);
    }

    private static byte[] palette() throws java.io.IOException {
        byte[] rows = {0, 0, 1, 2, 0, 2, 0, 1, 0, 1};
        return png(4, 2, 8, 3, rows, new byte[] {(byte) 255, 0, 0, 0, (byte) 255, 0, 0, 0,
            (byte) 255});
    }

    private static byte[] png(int width, int height, int depth, int colorType, byte[] rows,
                              byte[] palette) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

        ByteArrayOutputStream header = new ByteArrayOutputStream();
        writeInt(header, width);
        writeInt(header, height);
        header.write(depth);
        header.write(colorType);
        header.write(0);
        header.write(0);
        header.write(0);
        chunk(out, "IHDR", header.toByteArray());
        if (palette != null) {
            chunk(out, "PLTE", palette);
        }

        java.util.zip.Deflater packer = new java.util.zip.Deflater();
        packer.setInput(rows);
        packer.finish();
        ByteArrayOutputStream packed = new ByteArrayOutputStream();
        byte[] room = new byte[256];
        while (!packer.finished()) {
            packed.write(room, 0, packer.deflate(room));
        }
        packer.end();
        chunk(out, "IDAT", packed.toByteArray());
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream out, String kind, byte[] body)
            throws java.io.IOException {
        byte[] name = kind.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        writeInt(out, body.length);
        out.write(name);
        out.write(body);
        java.util.zip.CRC32 check = new java.util.zip.CRC32();
        check.update(name);
        check.update(body);
        writeInt(out, (int) check.getValue());
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write(value >>> 24);
        out.write(value >>> 16);
        out.write(value >>> 8);
        out.write(value);
    }

    private static String hex(int argb) {
        return Integer.toHexString(argb).toUpperCase(java.util.Locale.ROOT);
    }

    private static boolean samePixels(BufferedImage left, BufferedImage right) {
        if (left.getWidth() != right.getWidth() || left.getHeight() != right.getHeight()) {
            return false;
        }
        for (int y = 0; y < left.getHeight(); y++) {
            for (int x = 0; x < left.getWidth(); x++) {
                if (left.getRGB(x, y) != right.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }
}
