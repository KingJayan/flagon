package java.awt;

import java.awt.geom.Rectangle2D;

public class Rectangle extends Rectangle2D implements Shape {

    public int x;
    public int y;
    public int width;
    public int height;

    public Rectangle() {
    }

    public Rectangle(Rectangle other) {
        this(other.x, other.y, other.width, other.height);
    }

    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle(int width, int height) {
        this(0, 0, width, height);
    }

    public Rectangle(Point location, Dimension size) {
        this(location.x, location.y, size.width, size.height);
    }

    public Rectangle(Point location) {
        this(location.x, location.y, 0, 0);
    }

    public Rectangle(Dimension size) {
        this(0, 0, size.width, size.height);
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getWidth() {
        return width;
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    @Override
    public Rectangle2D getBounds2D() {
        return new Rectangle(x, y, width, height);
    }

    public void setBounds(Rectangle other) {
        setBounds(other.x, other.y, other.width, other.height);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void setRect(double x, double y, double width, double height) {
        int left = clip(x, false);
        int top = clip(y, false);
        int right = clip(x + width, true);
        int bottom = clip(y + height, true);
        setBounds(left, top, clip((double) right - left, false),
                clip((double) bottom - top, false));
    }

    private static int clip(double value, boolean up) {
        if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) (up ? Math.ceil(value) : Math.floor(value));
    }

    public Point getLocation() {
        return new Point(x, y);
    }

    public void setLocation(Point point) {
        setLocation(point.x, point.y);
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void translate(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public Dimension getSize() {
        return new Dimension(width, height);
    }

    public void setSize(Dimension size) {
        setSize(size.width, size.height);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean contains(Point point) {
        return contains(point.x, point.y);
    }

    public boolean contains(int px, int py) {
        return (width | height) >= 0 && px >= x && py >= y && px < (long) x + width
                && py < (long) y + height;
    }

    public boolean contains(Rectangle other) {
        return contains(other.x, other.y, other.width, other.height);
    }

    public boolean contains(int px, int py, int rw, int rh) {
        if ((width | height | rw | rh) < 0) {
            return false;
        }
        if (px < x || py < y || rw == 0 || rh == 0) {
            return false;
        }
        return (long) px + rw <= (long) x + width && (long) py + rh <= (long) y + height;
    }

    public boolean intersects(Rectangle other) {
        if (other.width <= 0 || other.height <= 0 || width <= 0 || height <= 0) {
            return false;
        }
        return (long) other.x + other.width > x && (long) other.y + other.height > y
                && (long) x + width > other.x && (long) y + height > other.y;
    }

    public Rectangle intersection(Rectangle other) {
        int left = Math.max(x, other.x);
        int top = Math.max(y, other.y);
        long right = Math.min((long) x + width, (long) other.x + other.width);
        long bottom = Math.min((long) y + height, (long) other.y + other.height);
        return new Rectangle(left, top, (int) Math.max(right - left, Integer.MIN_VALUE),
                (int) Math.max(bottom - top, Integer.MIN_VALUE));
    }

    public Rectangle union(Rectangle other) {
        if ((width | height) < 0) {
            return new Rectangle(other);
        }
        if ((other.width | other.height) < 0) {
            return new Rectangle(this);
        }
        int left = Math.min(x, other.x);
        int top = Math.min(y, other.y);
        long right = Math.max((long) x + width, (long) other.x + other.width);
        long bottom = Math.max((long) y + height, (long) other.y + other.height);
        return new Rectangle(left, top, (int) Math.min(right - left, Integer.MAX_VALUE),
                (int) Math.min(bottom - top, Integer.MAX_VALUE));
    }

    public void add(int px, int py) {
        if ((width | height) < 0) {
            setBounds(px, py, 0, 0);
            return;
        }
        int left = Math.min(x, px);
        int top = Math.min(y, py);
        long right = Math.max((long) x + width, px);
        long bottom = Math.max((long) y + height, py);
        setBounds(left, top, (int) Math.min(right - left, Integer.MAX_VALUE),
                (int) Math.min(bottom - top, Integer.MAX_VALUE));
    }

    public void add(Point point) {
        add(point.x, point.y);
    }

    public void add(Rectangle other) {
        setBounds(union(other));
    }

    public void grow(int horizontal, int vertical) {
        long left = (long) x - horizontal;
        long top = (long) y - vertical;
        long newWidth = (long) width + 2L * horizontal;
        long newHeight = (long) height + 2L * vertical;
        setBounds(clamp(left), clamp(top), clamp(newWidth), clamp(newHeight));
    }

    private static int clamp(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    @Override
    public boolean isEmpty() {
        return width <= 0 || height <= 0;
    }

    @Override
    public int outcode(double px, double py) {
        int out = 0;
        if (width <= 0) {
            out |= OUT_LEFT | OUT_RIGHT;
        } else if (px < x) {
            out |= OUT_LEFT;
        } else if (px > x + (double) width) {
            out |= OUT_RIGHT;
        }
        if (height <= 0) {
            out |= OUT_TOP | OUT_BOTTOM;
        } else if (py < y) {
            out |= OUT_TOP;
        } else if (py > y + (double) height) {
            out |= OUT_BOTTOM;
        }
        return out;
    }

    @Override
    public Rectangle2D createIntersection(Rectangle2D other) {
        if (other instanceof Rectangle rectangle) {
            return intersection(rectangle);
        }
        Rectangle2D into = new Rectangle2D.Double();
        Rectangle2D.intersect(this, other, into);
        return into;
    }

    @Override
    public Rectangle2D createUnion(Rectangle2D other) {
        if (other instanceof Rectangle rectangle) {
            return union(rectangle);
        }
        Rectangle2D into = new Rectangle2D.Double();
        Rectangle2D.union(this, other, into);
        return into;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Rectangle rectangle) {
            return x == rectangle.x && y == rectangle.y && width == rectangle.width
                    && height == rectangle.height;
        }
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + "[x=" + x + ",y=" + y + ",width=" + width + ",height="
                + height + "]";
    }
}
