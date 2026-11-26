package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.image.ImageObserver;
import java.util.Map;

public abstract class Graphics2D extends Graphics {

    protected Graphics2D() {
    }

    public abstract void draw(Shape shape);

    public abstract boolean drawImage(Image image, AffineTransform transform,
                                      ImageObserver observer);

    @Override
    public abstract void drawString(String text, int x, int y);

    public abstract void drawString(String text, float x, float y);

    public abstract void fill(Shape shape);

    public abstract void setComposite(Composite composite);

    public abstract void setPaint(Paint paint);

    public abstract void setStroke(Stroke stroke);

    public abstract void setRenderingHint(RenderingHints.Key key, Object value);

    public abstract Object getRenderingHint(RenderingHints.Key key);

    public abstract void setRenderingHints(Map<?, ?> hints);

    public abstract void addRenderingHints(Map<?, ?> hints);

    public abstract RenderingHints getRenderingHints();

    @Override
    public abstract void translate(int x, int y);

    public abstract void translate(double tx, double ty);

    public abstract void rotate(double theta);

    public abstract void rotate(double theta, double x, double y);

    public abstract void scale(double sx, double sy);

    public abstract void shear(double shx, double shy);

    public abstract void transform(AffineTransform transform);

    public abstract void setTransform(AffineTransform transform);

    public abstract AffineTransform getTransform();

    public abstract Paint getPaint();

    public abstract Composite getComposite();

    public abstract void setBackground(Color color);

    public abstract Color getBackground();

    public abstract Stroke getStroke();

    public abstract void clip(Shape shape);
}
