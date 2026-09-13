package java.awt.image;

/** an image something can read the pixels of */
public interface RenderedImage {

    int getWidth();

    int getHeight();

    Raster getData();
}
