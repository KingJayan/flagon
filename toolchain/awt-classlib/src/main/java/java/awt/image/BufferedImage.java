package java.awt.image;

import java.awt.Graphics;
import java.awt.Image;

public class BufferedImage extends Image implements RenderedImage {

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

    public WritableRaster getRaster() {
        return raster;
    }

    public WritableRaster getAlphaRaster() {
        return raster;
    }

    @Override
    public Raster getData() {
        return raster;
    }

    public int getRGB(int x, int y) {
        requireInside(x, y);
        return pixels()[y * width + x];
    }

    public void setRGB(int x, int y, int argb) {
        requireInside(x, y);
        pixels()[y * width + x] = argb;
    }

    public int[] getRGB(int x, int y, int rectWidth, int rectHeight, int[] into, int offset,
                        int stride) {
        requireRect(x, y, rectWidth, rectHeight);
        int[] out = into == null ? new int[offset + rectHeight * stride] : into;
        int[] source = pixels();
        for (int row = 0; row < rectHeight; row++) {
            System.arraycopy(source, (y + row) * width + x, out, offset + row * stride,
                    rectWidth);
        }
        return out;
    }

    public void setRGB(int x, int y, int rectWidth, int rectHeight, int[] from, int offset,
                       int stride) {
        requireRect(x, y, rectWidth, rectHeight);
        int[] target = pixels();
        for (int row = 0; row < rectHeight; row++) {
            System.arraycopy(from, offset + row * stride, target, (y + row) * width + x,
                    rectWidth);
        }
    }

    public BufferedImage getSubimage(int x, int y, int rectWidth, int rectHeight) {
        BufferedImage part = new BufferedImage(rectWidth, rectHeight, type);
        part.setRGB(0, 0, rectWidth, rectHeight,
                getRGB(x, y, rectWidth, rectHeight, null, 0, rectWidth), 0, rectWidth);
        return part;
    }

    public Graphics getGraphics() {
        return createGraphics();
    }

    public Graphics createGraphics() {
        return new ImageGraphics(this);
    }

    int[] pixels() {
        return ((DataBufferInt) raster.getDataBuffer()).getData();
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
