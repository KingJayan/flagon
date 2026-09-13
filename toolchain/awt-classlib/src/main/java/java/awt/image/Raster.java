package java.awt.image;

/** the pixels of an image, read only */
public class Raster {

    protected final DataBufferInt buffer;
    protected final int width;
    protected final int height;

    Raster(DataBufferInt buffer, int width, int height) {
        this.buffer = buffer;
        this.width = width;
        this.height = height;
    }

    public DataBuffer getDataBuffer() {
        return buffer;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getNumBands() {
        return 4;
    }
}
