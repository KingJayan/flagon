package java.awt.geom;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Arrays;

public abstract class Path2D implements Shape, Cloneable {

    public static final int WIND_EVEN_ODD = PathIterator.WIND_EVEN_ODD;
    public static final int WIND_NON_ZERO = PathIterator.WIND_NON_ZERO;

    private static final String NO_MOVE = "missing initial moveto in path definition";

    public static class Float extends Path2D {

        public Float() {
            this(WIND_NON_ZERO);
        }

        public Float(int rule) {
            super(rule, 20);
        }

        public Float(int rule, int capacity) {
            super(rule, capacity);
        }

        public Float(Shape shape) {
            this(shape, null);
        }

        public Float(Shape shape, AffineTransform transform) {
            super(WIND_NON_ZERO, 20);
            PathIterator path = shape.getPathIterator(transform);
            setWindingRule(path.getWindingRule());
            append(path, false);
        }

        @Override
        double round(double value) {
            return (float) value;
        }

        public final void moveTo(float x, float y) {
            moveTo((double) x, (double) y);
        }

        public final void lineTo(float x, float y) {
            lineTo((double) x, (double) y);
        }

        public final void quadTo(float x1, float y1, float x2, float y2) {
            quadTo((double) x1, (double) y1, (double) x2, (double) y2);
        }

        public final void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            curveTo((double) x1, (double) y1, (double) x2, (double) y2, (double) x3,
                    (double) y3);
        }

        @Override
        public Object clone() {
            return copyInto(new Float(getWindingRule(), types.length));
        }
    }

    public static class Double extends Path2D {

        public Double() {
            this(WIND_NON_ZERO);
        }

        public Double(int rule) {
            super(rule, 20);
        }

        public Double(int rule, int capacity) {
            super(rule, capacity);
        }

        public Double(Shape shape) {
            this(shape, null);
        }

        public Double(Shape shape, AffineTransform transform) {
            super(WIND_NON_ZERO, 20);
            PathIterator path = shape.getPathIterator(transform);
            setWindingRule(path.getWindingRule());
            append(path, false);
        }

        @Override
        double round(double value) {
            return value;
        }

        @Override
        public Object clone() {
            return copyInto(new Double(getWindingRule(), types.length));
        }
    }

    int[] types;
    double[] coords;
    int typeCount;
    int coordCount;
    private int rule;

    Path2D(int rule, int capacity) {
        setWindingRule(rule);
        this.types = new int[Math.max(capacity, 1)];
        this.coords = new double[Math.max(capacity, 1) * 2];
    }

    abstract double round(double value);

    Path2D copyInto(Path2D copy) {
        copy.types = Arrays.copyOf(types, types.length);
        copy.coords = Arrays.copyOf(coords, coords.length);
        copy.typeCount = typeCount;
        copy.coordCount = coordCount;
        return copy;
    }

    private void add(int type, double... points) {
        if (type != PathIterator.SEG_MOVETO && typeCount == 0) {
            throw new IllegalPathStateException(NO_MOVE);
        }
        if (typeCount == types.length) {
            types = Arrays.copyOf(types, types.length * 2);
        }
        if (coordCount + points.length > coords.length) {
            coords = Arrays.copyOf(coords, Math.max(coords.length * 2,
                    coordCount + points.length));
        }
        types[typeCount++] = type;
        for (double value : points) {
            coords[coordCount++] = round(value);
        }
    }

    public void moveTo(double x, double y) {
        if (typeCount > 0 && types[typeCount - 1] == PathIterator.SEG_MOVETO) {
            coords[coordCount - 2] = round(x);
            coords[coordCount - 1] = round(y);
        } else {
            add(PathIterator.SEG_MOVETO, x, y);
        }
    }

    public void lineTo(double x, double y) {
        add(PathIterator.SEG_LINETO, x, y);
    }

    public void quadTo(double x1, double y1, double x2, double y2) {
        add(PathIterator.SEG_QUADTO, x1, y1, x2, y2);
    }

    public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
        add(PathIterator.SEG_CUBICTO, x1, y1, x2, y2, x3, y3);
    }

    public final void closePath() {
        if (typeCount == 0 || types[typeCount - 1] != PathIterator.SEG_CLOSE) {
            add(PathIterator.SEG_CLOSE);
        }
    }

    public final void append(Shape shape, boolean connect) {
        append(shape.getPathIterator(null), connect);
    }

    public void append(PathIterator path, boolean connect) {
        double[] point = new double[6];
        while (!path.isDone()) {
            switch (path.currentSegment(point)) {
                case PathIterator.SEG_MOVETO -> {
                    if (!connect || typeCount < 1 || coordCount < 1) {
                        moveTo(point[0], point[1]);
                    } else if (types[typeCount - 1] == PathIterator.SEG_CLOSE
                            || coords[coordCount - 2] != point[0]
                            || coords[coordCount - 1] != point[1]) {
                        lineTo(point[0], point[1]);
                    }
                }
                case PathIterator.SEG_LINETO -> lineTo(point[0], point[1]);
                case PathIterator.SEG_QUADTO -> quadTo(point[0], point[1], point[2], point[3]);
                case PathIterator.SEG_CUBICTO ->
                        curveTo(point[0], point[1], point[2], point[3], point[4], point[5]);
                default -> closePath();
            }
            path.next();
            connect = false;
        }
    }

    public final int getWindingRule() {
        return rule;
    }

    public final void setWindingRule(int rule) {
        if (rule != WIND_EVEN_ODD && rule != WIND_NON_ZERO) {
            throw new IllegalArgumentException("winding rule must be WIND_EVEN_ODD or "
                    + "WIND_NON_ZERO");
        }
        this.rule = rule;
    }

    public final Point2D getCurrentPoint() {
        if (typeCount < 1 || coordCount < 1) {
            return null;
        }
        if (types[typeCount - 1] == PathIterator.SEG_CLOSE) {
            for (int i = typeCount - 2, at = coordCount; i > 0; i--) {
                at -= Segments.points(types[i]) * 2;
                if (types[i] == PathIterator.SEG_MOVETO) {
                    return point(coords[at], coords[at + 1]);
                }
            }
            return point(coords[0], coords[1]);
        }
        return point(coords[coordCount - 2], coords[coordCount - 1]);
    }

    private Point2D point(double x, double y) {
        return this instanceof Float ? new Point2D.Float((float) x, (float) y)
                : new Point2D.Double(x, y);
    }

    public final void reset() {
        typeCount = 0;
        coordCount = 0;
    }

    public void transform(AffineTransform transform) {
        transform.transform(coords, 0, coords, 0, coordCount / 2);
        for (int i = 0; i < coordCount; i++) {
            coords[i] = round(coords[i]);
        }
    }

    public final Shape createTransformedShape(AffineTransform transform) {
        Path2D copy = (Path2D) clone();
        if (transform != null) {
            copy.transform(transform);
        }
        return copy;
    }

    @Override
    public final Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        if (coordCount == 0) {
            return this instanceof Float ? new Rectangle2D.Float() : new Rectangle2D.Double();
        }
        double[] box = {coords[0], coords[1], coords[0], coords[1]};
        double lastX = 0;
        double lastY = 0;
        for (int i = 0, at = 0; i < typeCount; i++) {
            int type = types[i];
            switch (type) {
                case PathIterator.SEG_QUADTO -> {
                    extend(box, quadExtremes(lastX, coords[at], coords[at + 2]), lastX,
                            coords[at], coords[at + 2], lastY, coords[at + 1], coords[at + 3]);
                }
                case PathIterator.SEG_CUBICTO -> {
                    for (double t : cubicExtremes(lastX, coords[at], coords[at + 2],
                            coords[at + 4])) {
                        include(box, cubic(lastX, coords[at], coords[at + 2], coords[at + 4], t),
                                cubic(lastY, coords[at + 1], coords[at + 3], coords[at + 5], t));
                    }
                    for (double t : cubicExtremes(lastY, coords[at + 1], coords[at + 3],
                            coords[at + 5])) {
                        include(box, cubic(lastX, coords[at], coords[at + 2], coords[at + 4], t),
                                cubic(lastY, coords[at + 1], coords[at + 3], coords[at + 5], t));
                    }
                }
                default -> {
                }
            }
            int length = Segments.points(type) * 2;
            if (length > 0) {
                lastX = coords[at + length - 2];
                lastY = coords[at + length - 1];
                include(box, lastX, lastY);
            }
            at += length;
        }
        if (this instanceof Float) {
            return new Rectangle2D.Float((float) box[0], (float) box[1],
                    (float) (box[2] - box[0]), (float) (box[3] - box[1]));
        }
        return new Rectangle2D.Double(box[0], box[1], box[2] - box[0], box[3] - box[1]);
    }

    private static void extend(double[] box, double[] times, double x0, double cx, double x1,
                               double y0, double cy, double y1) {
        for (double t : times) {
            include(box, quad(x0, cx, x1, t), quad(y0, cy, y1, t));
        }
        for (double t : quadExtremes(y0, cy, y1)) {
            include(box, quad(x0, cx, x1, t), quad(y0, cy, y1, t));
        }
    }

    private static void include(double[] box, double x, double y) {
        box[0] = Math.min(box[0], x);
        box[1] = Math.min(box[1], y);
        box[2] = Math.max(box[2], x);
        box[3] = Math.max(box[3], y);
    }

    private static double quad(double p0, double p1, double p2, double t) {
        double u = 1 - t;
        return u * u * p0 + 2 * u * t * p1 + t * t * p2;
    }

    private static double cubic(double p0, double p1, double p2, double p3, double t) {
        double u = 1 - t;
        return u * u * u * p0 + 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t * p3;
    }

    private static double[] quadExtremes(double p0, double p1, double p2) {
        double divisor = p0 - 2 * p1 + p2;
        if (divisor == 0) {
            return new double[0];
        }
        double t = (p0 - p1) / divisor;
        return t > 0 && t < 1 ? new double[] {t} : new double[0];
    }

    private static double[] cubicExtremes(double p0, double p1, double p2, double p3) {
        double a = -p0 + 3 * p1 - 3 * p2 + p3;
        double b = 2 * (p0 - 2 * p1 + p2);
        double c = p1 - p0;
        double[] found = new double[2];
        int count = 0;
        if (a == 0) {
            if (b != 0) {
                found[count++] = -c / b;
            }
        } else {
            double discriminant = b * b - 4 * a * c;
            if (discriminant >= 0) {
                double root = Math.sqrt(discriminant);
                found[count++] = (-b + root) / (2 * a);
                found[count++] = (-b - root) / (2 * a);
            }
        }
        double[] inside = new double[count];
        int kept = 0;
        for (int i = 0; i < count; i++) {
            if (found[i] > 0 && found[i] < 1) {
                inside[kept++] = found[i];
            }
        }
        return Arrays.copyOf(inside, kept);
    }

    @Override
    public final boolean contains(double x, double y) {
        if (!(x * 0 + y * 0 == 0)) {
            return false;
        }
        int crossings = Crossings.around(getPathIterator(null, Crossings.FLATNESS), x, y);
        return (rule == WIND_NON_ZERO ? crossings : crossings & 1) != 0;
    }

    @Override
    public final boolean contains(Point2D point) {
        return contains(point.getX(), point.getY());
    }

    @Override
    public final boolean intersects(double x, double y, double width, double height) {
        return Crossings.intersects(this, x, y, width, height);
    }

    @Override
    public final boolean intersects(Rectangle2D rectangle) {
        return intersects(rectangle.getX(), rectangle.getY(), rectangle.getWidth(),
                rectangle.getHeight());
    }

    @Override
    public final boolean contains(double x, double y, double width, double height) {
        return Crossings.contains(this, x, y, width, height);
    }

    @Override
    public final boolean contains(Rectangle2D rectangle) {
        return contains(rectangle.getX(), rectangle.getY(), rectangle.getWidth(),
                rectangle.getHeight());
    }

    @Override
    public final PathIterator getPathIterator(AffineTransform transform) {
        return new Segments(types, coords, typeCount, rule, transform);
    }

    @Override
    public final PathIterator getPathIterator(AffineTransform transform, double flatness) {
        return new FlatteningPathIterator(getPathIterator(transform), flatness);
    }

    @Override
    public abstract Object clone();
}
