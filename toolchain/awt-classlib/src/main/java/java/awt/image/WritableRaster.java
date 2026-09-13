package java.awt.image;

/** the pixels of an image, which can be written back */
public class WritableRaster extends Raster {

    WritableRaster(DataBufferInt buffer, int width, int height) {
        super(buffer, width, height);
    }

    public void setDataElements(int x, int y, Object pixel) {
        if (!(pixel instanceof int[] value) || value.length < 1) {
            throw new IllegalArgumentException("a pixel here is one int of 0xAARRGGBB");
        }
        buffer.setElem(y * width + x, value[0]);
    }

    public Object getDataElements(int x, int y, Object into) {
        int[] pixel = into instanceof int[] given && given.length >= 1 ? given : new int[1];
        pixel[0] = buffer.getElem(y * width + x);
        return pixel;
    }
}
