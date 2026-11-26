package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

/**
 * prints the graphics checks, which the browser test compares with what the jdk printed, then
 * the drawing only a browser does: text, antialiased edges and webp
 */
public final class GraphicsProgram implements Program {

    /** 2x1 of 0x3366cc, lossless */
    private static final String WEBP = "UklGRh4AAABXRUJQVlA4TBEAAAAvAQAAAAdQs86Uuf+BiOh/AAA=";

    @Override
    public String id() {
        return "graphics";
    }

    @Override
    public String description() {
        return "java.awt shapes, text and transforms on a canvas, and images the browser decodes";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        ConsoleSink console = Platforms.get().console();
        for (String line : GraphicsChecks.run()) {
            console.info("graphics: " + line);
        }

        BufferedImage page = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = page.createGraphics();
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        graphics.drawString("Hello", 2, 24);
        console.info("graphics: text paints " + (painted(page) > 40));
        FontMetrics metrics = graphics.getFontMetrics();
        console.info("graphics: metrics "
                + (metrics.stringWidth("WWWW") > metrics.stringWidth("iiii")) + " "
                + (metrics.getAscent() > 10) + " " + (metrics.getHeight() >= 20));

        BufferedImage oval = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D painting = oval.createGraphics();
        painting.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        painting.setColor(Color.RED);
        painting.fillOval(0, 0, 20, 20);
        int blended = 0;
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 20; x++) {
                int alpha = oval.getRGB(x, y) >>> 24;
                blended += alpha > 0 && alpha < 255 ? 1 : 0;
            }
        }
        console.info("graphics: an oval fills " + (oval.getRGB(10, 10) == 0xFFFF0000)
                + " misses its corner " + (oval.getRGB(0, 0) >>> 24 == 0) + " blends its edge "
                + (blended > 20));

        BufferedImage strip = new BufferedImage(4, 1, BufferedImage.TYPE_INT_ARGB);
        strip.setRGB(0, 0, 4, 1, new int[] {0xFF000000, 0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF},
                0, 4);
        BufferedImage smooth = new BufferedImage(16, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D smoothing = smooth.createGraphics();
        smoothing.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        smoothing.drawImage(strip, 0, 0, 16, 1, null);
        int middle = smooth.getRGB(4, 0) & 0xFF;
        console.info("graphics: a bilinear stretch blends " + (middle > 0 && middle < 255));

        BufferedImage webp = ImageIO.read(new ByteArrayInputStream(
                Base64.getDecoder().decode(WEBP)));
        console.info("graphics: webp " + webp.getWidth() + "x" + webp.getHeight() + " "
                + Integer.toHexString(webp.getRGB(1, 0)));
        console.info("graphics: done");
    }

    private static int painted(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) >>> 24 != 0) {
                    count++;
                }
            }
        }
        return count;
    }
}
