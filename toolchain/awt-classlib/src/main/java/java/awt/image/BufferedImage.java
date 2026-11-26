package java.awt.image;

import dev.flagon.platform.Platforms;
import dev.flagon.platform.graphics.Canvas2D;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Transparency;

/**
 * one 0xAARRGGBB int per pixel whatever the type; an opaque type keeps no alpha bits, as the
 * jdk's do. shapes and text are drawn on a canvas copy, which is sent the pixels again whenever
 * java may have changed them.
 */
public class BufferedImage extends Image implements RenderedImage, Transparency {

    public static final int TYPE_INT_RGB = 1;
    public static final int TYPE_INT_ARGB = 2;
    public static final int TYPE_INT_ARGB_PRE = 3;
    public static final int TYPE_INT_BGR = 4;
    public static final int TYPE_3BYTE_BGR = 5;
    public static final int TYPE_4BYTE_ABGR = 6;
    public static final int TYPE_BYTE_GRAY = 10;

    private final int width;
    private final int height;
    private final int type;
    private final WritableRaster raster;
    private Canvas2D surface;
    private boolean surfaceCurrent;
    private boolean shared;

    public BufferedImage(int width, int height, int type) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "an image of " + width + "x" + height + " holds no pixels");
        }
        this.width = width;
        this.height = height;
        this.type = type;
        this.raster = new WritableRaster(new DataBufferInt(width * height), width, height);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth(ImageObserver observer) {
        return width;
    }

    @Override
    public int getHeight(ImageObserver observer) {
        return height;
    }

    public int getType() {
        return type;
    }

    @Override
    public int getTransparency() {
        return opaque() ? OPAQUE : TRANSLUCENT;
    }

    public WritableRaster getRaster() {
        shared = true;
        return raster;
    }

    public WritableRaster getAlphaRaster() {
        return getRaster();
    }

    @Override
    public Raster getData() {
        int[] copy = pixels().clone();
        return new Raster(new DataBufferInt(copy, copy.length), width, height);
    }

    public int getRGB(int x, int y) {
        requireInside(x, y);
        return read(pixels()[y * width + x]);
    }

    public void setRGB(int x, int y, int argb) {
        requireInside(x, y);
        pixels()[y * width + x] = stored(argb);
        changed();
    }

    public int[] getRGB(int x, int y, int rectWidth, int rectHeight, int[] into, int offset,
                        int stride) {
        requireRect(x, y, rectWidth, rectHeight);
        int[] out = into == null ? new int[offset + rectHeight * stride] : into;
        int[] source = pixels();
        for (int row = 0; row < rectHeight; row++) {
            int from = (y + row) * width + x;
            int to = offset + row * stride;
            for (int column = 0; column < rectWidth; column++) {
                out[to + column] = read(source[from + column]);
            }
        }
        return out;
    }

    public void setRGB(int x, int y, int rectWidth, int rectHeight, int[] from, int offset,
                       int stride) {
        requireRect(x, y, rectWidth, rectHeight);
        int[] target = pixels();
        for (int row = 0; row < rectHeight; row++) {
            int to = (y + row) * width + x;
            int at = offset + row * stride;
            for (int column = 0; column < rectWidth; column++) {
                target[to + column] = stored(from[at + column]);
            }
        }
        changed();
    }

    public BufferedImage getSubimage(int x, int y, int rectWidth, int rectHeight) {
        BufferedImage part = new BufferedImage(rectWidth, rectHeight, type);
        part.setRGB(0, 0, rectWidth, rectHeight,
                getRGB(x, y, rectWidth, rectHeight, null, 0, rectWidth), 0, rectWidth);
        return part;
    }

    @Override
    public Graphics getGraphics() {
        return createGraphics();
    }

    public Graphics2D createGraphics() {
        return new ImageGraphics(this);
    }

    int[] pixels() {
        return ((DataBufferInt) raster.getDataBuffer()).getData();
    }

    boolean opaque() {
        return type == TYPE_INT_RGB || type == TYPE_INT_BGR || type == TYPE_3BYTE_BGR
                || type == TYPE_BYTE_GRAY;
    }

    int read(int stored) {
        return opaque() ? stored | 0xFF000000 : stored;
    }

    int stored(int argb) {
        return opaque() ? argb & 0x00FFFFFF : argb;
    }

    /** java wrote the pixels, so the canvas copy is out of date */
    void changed() {
        surfaceCurrent = false;
    }

    /** @return the canvas copy, holding the pixels as they are now */
    Canvas2D surface() {
        if (surface == null) {
            surface = Platforms.get().drawing().canvas(width, height);
        }
        if (!surfaceCurrent || shared) {
            int[] pixels = pixels();
            if (opaque()) {
                pixels = pixels.clone();
                for (int at = 0; at < pixels.length; at++) {
                    pixels[at] |= 0xFF000000;
                }
            }
            surface.put(pixels, 0, width, 0, 0, width, height);
            surfaceCurrent = true;
        }
        return surface;
    }

    /** copies what the canvas drew in this area back into the pixels */
    void drawn(int x, int y, int areaWidth, int areaHeight) {
        int[] pixels = pixels();
        surface.get(pixels, y * width + x, width, x, y, areaWidth, areaHeight);
        if (opaque()) {
            for (int row = y; row < y + areaHeight; row++) {
                for (int at = row * width + x; at < row * width + x + areaWidth; at++) {
                    pixels[at] &= 0x00FFFFFF;
                }
            }
        }
    }

    private void requireInside(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new ArrayIndexOutOfBoundsException(
                    "(" + x + ", " + y + ") is outside a " + width + "x" + height + " image");
        }
    }

    private void requireRect(int x, int y, int rectWidth, int rectHeight) {
        if (x < 0 || y < 0 || rectWidth < 0 || rectHeight < 0
                || x + rectWidth > width || y + rectHeight > height) {
            throw new ArrayIndexOutOfBoundsException(
                    rectWidth + "x" + rectHeight + " at (" + x + ", " + y + ") is outside a "
                            + width + "x" + height + " image");
        }
    }
}
