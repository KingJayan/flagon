package java.awt;

import java.awt.image.ImageObserver;
import java.nio.charset.StandardCharsets;

public abstract class Graphics {

    protected Graphics() {
    }

    public abstract Graphics create();

    public Graphics create(int x, int y, int width, int height) {
        Graphics made = create();
        made.translate(x, y);
        made.clipRect(0, 0, width, height);
        return made;
    }

    public abstract void translate(int x, int y);

    public abstract Color getColor();

    public abstract void setColor(Color color);

    public abstract void setPaintMode();

    public abstract void setXORMode(Color color);

    public abstract Font getFont();

    public abstract void setFont(Font font);

    public FontMetrics getFontMetrics() {
        return getFontMetrics(getFont());
    }

    public abstract FontMetrics getFontMetrics(Font font);

    public abstract Rectangle getClipBounds();

    public abstract void clipRect(int x, int y, int width, int height);

    public abstract void setClip(int x, int y, int width, int height);

    public abstract Shape getClip();

    public abstract void setClip(Shape clip);

    public abstract void copyArea(int x, int y, int width, int height, int dx, int dy);

    public abstract void drawLine(int x1, int y1, int x2, int y2);

    public abstract void fillRect(int x, int y, int width, int height);

    public void drawRect(int x, int y, int width, int height) {
        if (width < 0 || height < 0) {
            return;
        }
        if (height == 0 || width == 0) {
            drawLine(x, y, x + width, y + height);
        } else {
            drawLine(x, y, x + width - 1, y);
            drawLine(x + width, y, x + width, y + height - 1);
            drawLine(x + width, y + height, x + 1, y + height);
            drawLine(x, y + height, x, y + 1);
        }
    }

    public abstract void clearRect(int x, int y, int width, int height);

    public abstract void drawRoundRect(int x, int y, int width, int height, int arcWidth,
                                       int arcHeight);

    public abstract void fillRoundRect(int x, int y, int width, int height, int arcWidth,
                                       int arcHeight);

    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        Color color = getColor();
        Color brighter = color.brighter();
        Color darker = color.darker();
        setColor(raised ? brighter : darker);
        drawLine(x, y, x, y + height);
        drawLine(x + 1, y, x + width - 1, y);
        setColor(raised ? darker : brighter);
        drawLine(x + 1, y + height, x + width, y + height);
        drawLine(x + width, y, x + width, y + height - 1);
        setColor(color);
    }

    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        Color color = getColor();
        Color brighter = color.brighter();
        Color darker = color.darker();
        if (!raised) {
            setColor(darker);
        }
        fillRect(x + 1, y + 1, width - 2, height - 2);
        setColor(raised ? brighter : darker);
        drawLine(x, y, x, y + height - 1);
        drawLine(x + 1, y, x + width - 2, y);
        setColor(raised ? darker : brighter);
        drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
        drawLine(x + width - 1, y, x + width - 1, y + height - 2);
        setColor(color);
    }

    public abstract void drawOval(int x, int y, int width, int height);

    public abstract void fillOval(int x, int y, int width, int height);

    public abstract void drawArc(int x, int y, int width, int height, int startAngle,
                                 int arcAngle);

    public abstract void fillArc(int x, int y, int width, int height, int startAngle,
                                 int arcAngle);

    public abstract void drawPolyline(int[] xPoints, int[] yPoints, int count);

    public abstract void drawPolygon(int[] xPoints, int[] yPoints, int count);

    public void drawPolygon(Polygon polygon) {
        drawPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
    }

    public abstract void fillPolygon(int[] xPoints, int[] yPoints, int count);

    public void fillPolygon(Polygon polygon) {
        fillPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
    }

    public abstract void drawString(String text, int x, int y);

    public void drawChars(char[] text, int offset, int length, int x, int y) {
        drawString(new String(text, offset, length), x, y);
    }

    public void drawBytes(byte[] text, int offset, int length, int x, int y) {
        drawString(new String(text, offset, length, StandardCharsets.ISO_8859_1), x, y);
    }

    public abstract boolean drawImage(Image image, int x, int y, ImageObserver observer);

    public abstract boolean drawImage(Image image, int x, int y, int width, int height,
                                      ImageObserver observer);

    public abstract boolean drawImage(Image image, int x, int y, Color background,
                                      ImageObserver observer);

    public abstract boolean drawImage(Image image, int x, int y, int width, int height,
                                      Color background, ImageObserver observer);

    public abstract boolean drawImage(Image image, int toX1, int toY1, int toX2, int toY2,
                                      int fromX1, int fromY1, int fromX2, int fromY2,
                                      ImageObserver observer);

    public abstract boolean drawImage(Image image, int toX1, int toY1, int toX2, int toY2,
                                      int fromX1, int fromY1, int fromX2, int fromY2,
                                      Color background, ImageObserver observer);

    public abstract void dispose();

    @Deprecated
    public Rectangle getClipRect() {
        return getClipBounds();
    }

    public boolean hitClip(int x, int y, int width, int height) {
        Rectangle clip = getClipBounds();
        return clip == null || clip.intersects(new Rectangle(x, y, width, height));
    }

    public Rectangle getClipBounds(Rectangle into) {
        Rectangle clip = getClipBounds();
        if (clip != null) {
            into.setBounds(clip);
        }
        return into;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[font=" + getFont() + ",color=" + getColor() + "]";
    }
}
