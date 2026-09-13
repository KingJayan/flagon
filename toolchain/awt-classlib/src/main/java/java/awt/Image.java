package java.awt;

/**
 * a rectangle of pixels, whatever holds them
 */
public abstract class Image {

    public static final int SCALE_DEFAULT = 1;
    public static final int SCALE_FAST = 2;
    public static final int SCALE_SMOOTH = 4;

    protected Image() {
    }

    public abstract int getWidth(java.awt.image.ImageObserver observer);

    public abstract int getHeight(java.awt.image.ImageObserver observer);

    public void flush() {
    }
}
