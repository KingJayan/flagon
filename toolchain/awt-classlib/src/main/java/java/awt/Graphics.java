package java.awt;

import java.awt.image.ImageObserver;

public abstract class Graphics {

    protected Graphics() {
    }

    public abstract boolean drawImage(Image image, int x, int y, ImageObserver observer);

    public abstract boolean drawImage(Image image, int toX1, int toY1, int toX2, int toY2,
                                      int fromX1, int fromY1, int fromX2, int fromY2,
                                      ImageObserver observer);

    public abstract void dispose();

    public void drawString(String text, int x, int y) {
        throw new UnsupportedOperationException(
                "a bundle carries no font and no rasteriser, so there is nothing to draw text "
                        + "with. draw the glyphs from a texture instead.");
    }

    public void fillRect(int x, int y, int width, int height) {
        throw new UnsupportedOperationException(
                "Graphics draws one image into another here, and nothing else");
    }

    public void drawRect(int x, int y, int width, int height) {
        fillRect(x, y, width, height);
    }
}
