package java.awt.image;

public final class DataBufferInt extends DataBuffer {

    private final int[] pixels;

    public DataBufferInt(int size) {
        super(TYPE_INT, size);
        this.pixels = new int[size];
    }

    public DataBufferInt(int[] pixels, int size) {
        super(TYPE_INT, size);
        this.pixels = pixels;
    }

    public int[] getData() {
        return pixels;
    }

    @Override
    public int getElem(int index) {
        return pixels[index];
    }

    @Override
    public void setElem(int index, int value) {
        pixels[index] = value;
    }
}
