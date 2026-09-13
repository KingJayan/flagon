package java.awt.image;

/** where a raster keeps its numbers */
public abstract class DataBuffer {

    public static final int TYPE_BYTE = 0;
    public static final int TYPE_INT = 3;

    private final int type;
    private final int size;

    protected DataBuffer(int type, int size) {
        this.type = type;
        this.size = size;
    }

    public int getDataType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public abstract int getElem(int index);

    public abstract void setElem(int index, int value);
}
