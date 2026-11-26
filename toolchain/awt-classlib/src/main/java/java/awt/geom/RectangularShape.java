package java.awt.geom;

import java.awt.Rectangle;
import java.awt.Shape;

public abstract class RectangularShape implements Shape, Cloneable {

    protected RectangularShape() {
    }

    public abstract double getX();

    public abstract double getY();

    public abstract double getWidth();

    public abstract double getHeight();

    public abstract boolean isEmpty();

    public abstract void setFrame(double x, double y, double width, double height);

    public double getMinX() {
        return getX();
    }

    public double getMinY() {
        return getY();
    }

    public double getMaxX() {
        return getX() + getWidth();
    }

    public double getMaxY() {
        return getY() + getHeight();
    }

    public double getCenterX() {
        return getX() + getWidth() / 2.0;
    }

    public double getCenterY() {
        return getY() + getHeight() / 2.0;
    }

    public Rectangle2D getFrame() {
        return new Rectangle2D.Double(getX(), getY(), getWidth(), getHeight());
    }

    public void setFrame(Point2D location, Dimension2D size) {
        setFrame(location.getX(), location.getY(), size.getWidth(), size.getHeight());
    }

    public void setFrame(Rectangle2D frame) {
        setFrame(frame.getX(), frame.getY(), frame.getWidth(), frame.getHeight());
    }

    public void setFrameFromDiagonal(double x1, double y1, double x2, double y2) {
        if (x2 < x1) {
            double swap = x1;
            x1 = x2;
            x2 = swap;
        }
        if (y2 < y1) {
            double swap = y1;
            y1 = y2;
            y2 = swap;
        }
        setFrame(x1, y1, x2 - x1, y2 - y1);
    }

    public void setFrameFromDiagonal(Point2D one, Point2D two) {
        setFrameFromDiagonal(one.getX(), one.getY(), two.getX(), two.getY());
    }

    public void setFrameFromCenter(double centerX, double centerY, double cornerX,
                                   double cornerY) {
        double halfWidth = Math.abs(cornerX - centerX);
        double halfHeight = Math.abs(cornerY - centerY);
        setFrame(centerX - halfWidth, centerY - halfHeight, halfWidth * 2.0, halfHeight * 2.0);
    }

    public void setFrameFromCenter(Point2D center, Point2D corner) {
        setFrameFromCenter(center.getX(), center.getY(), corner.getX(), corner.getY());
    }

    @Override
    public boolean contains(Point2D point) {
        return contains(point.getX(), point.getY());
    }

    @Override
    public boolean intersects(Rectangle2D rectangle) {
        return intersects(rectangle.getX(), rectangle.getY(), rectangle.getWidth(),
                rectangle.getHeight());
    }

    @Override
    public boolean contains(Rectangle2D rectangle) {
        return contains(rectangle.getX(), rectangle.getY(), rectangle.getWidth(),
                rectangle.getHeight());
    }

    @Override
    public Rectangle getBounds() {
        double width = getWidth();
        double height = getHeight();
        if (width < 0 || height < 0) {
            return new Rectangle();
        }
        double x = Math.floor(getX());
        double y = Math.floor(getY());
        double right = Math.ceil(getX() + width);
        double bottom = Math.ceil(getY() + height);
        return new Rectangle((int) x, (int) y, (int) (right - x), (int) (bottom - y));
    }

    @Override
    public PathIterator getPathIterator(AffineTransform transform, double flatness) {
        return new FlatteningPathIterator(getPathIterator(transform), flatness);
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException impossible) {
            throw new InternalError(impossible);
        }
    }
}
