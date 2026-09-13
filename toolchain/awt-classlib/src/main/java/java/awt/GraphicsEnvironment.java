package java.awt;

public abstract class GraphicsEnvironment {

    protected GraphicsEnvironment() {
    }

    public static boolean isHeadless() {
        return false;
    }

    public static GraphicsEnvironment getLocalGraphicsEnvironment() {
        throw new UnsupportedOperationException(
                "there is no awt screen device in a page: the canvas is the display, and "
                        + "org.lwjgl.opengl.Display opens it");
    }
}
