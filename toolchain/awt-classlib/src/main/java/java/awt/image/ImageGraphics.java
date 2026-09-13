package java.awt.image;

import java.awt.Graphics;
import java.awt.Image;

final class ImageGraphics extends Graphics {

    private final BufferedImage target;

    ImageGraphics(BufferedImage target) {
        this.target = target;
    }

    @Override
    public boolean drawImage(Image image, int x, int y, ImageObserver observer) {
        BufferedImage source = source(image);
        return drawImage(image, x, y, x + source.getWidth(), y + source.getHeight(),
                0, 0, source.getWidth(), source.getHeight(), observer);
    }

    @Override
    public boolean drawImage(Image image, int toX1, int toY1, int toX2, int toY2,
                             int fromX1, int fromY1, int fromX2, int fromY2,
                             ImageObserver observer) {
        BufferedImage source = source(image);
        int toWidth = toX2 - toX1;
        int toHeight = toY2 - toY1;
        int fromWidth = fromX2 - fromX1;
        int fromHeight = fromY2 - fromY1;
        if (toWidth <= 0 || toHeight <= 0 || fromWidth <= 0 || fromHeight <= 0) {
            return true;
        }
        int[] into = target.pixels();
        int[] out = source.pixels();
        for (int row = 0; row < toHeight; row++) {
            int y = toY1 + row;
            if (y < 0 || y >= target.getHeight()) {
                continue;
            }
            int sourceY = fromY1 + row * fromHeight / toHeight;
            for (int column = 0; column < toWidth; column++) {
                int x = toX1 + column;
                if (x < 0 || x >= target.getWidth()) {
                    continue;
                }
                int sourceX = fromX1 + column * fromWidth / toWidth;
                into[y * target.getWidth() + x] =
                        out[sourceY * source.getWidth() + sourceX];
            }
        }
        return true;
    }

    @Override
    public void dispose() {
    }

    private static BufferedImage source(Image image) {
        if (image instanceof BufferedImage buffered) {
            return buffered;
        }
        throw new UnsupportedOperationException(
                "Graphics draws a BufferedImage here; there is no other kind of image in a "
                        + "bundle");
    }
}
