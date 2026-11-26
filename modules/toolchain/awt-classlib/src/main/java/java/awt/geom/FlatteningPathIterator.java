package java.awt.geom;

/** a path with its curves cut into lines no further than {@code flatness} from the curve */
public class FlatteningPathIterator implements PathIterator {

    private final PathIterator source;
    private final double flatness;
    private final int limit;
    private final double[] point = new double[6];
    private double[] lines = new double[16];
    private int count;
    private int at;
    private int type;
    private double lastX;
    private double lastY;
    private boolean done;

    public FlatteningPathIterator(PathIterator source, double flatness) {
        this(source, flatness, 10);
    }

    public FlatteningPathIterator(PathIterator source, double flatness, int limit) {
        if (flatness < 0.0) {
            throw new IllegalArgumentException("flatness must be >= 0");
        }
        if (limit < 0) {
            throw new IllegalArgumentException("limit must be >= 0");
        }
        this.source = source;
        this.flatness = flatness;
        this.limit = limit;
        read();
    }

    public double getFlatness() {
        return flatness;
    }

    public int getRecursionLimit() {
        return limit;
    }

    @Override
    public int getWindingRule() {
        return source.getWindingRule();
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public void next() {
        if (type == SEG_LINETO && at + 2 < count) {
            at += 2;
            return;
        }
        source.next();
        read();
    }

    @Override
    public int currentSegment(double[] coords) {
        if (done) {
            throw new java.util.NoSuchElementException("the path has no more segments");
        }
        if (type != SEG_CLOSE) {
            coords[0] = lines[at];
            coords[1] = lines[at + 1];
        }
        return type;
    }

    @Override
    public int currentSegment(float[] coords) {
        if (done) {
            throw new java.util.NoSuchElementException("the path has no more segments");
        }
        if (type != SEG_CLOSE) {
            coords[0] = (float) lines[at];
            coords[1] = (float) lines[at + 1];
        }
        return type;
    }

    private void read() {
        if (source.isDone()) {
            done = true;
            return;
        }
        int segment = source.currentSegment(point);
        count = 0;
        at = 0;
        switch (segment) {
            case SEG_MOVETO, SEG_LINETO -> {
                type = segment;
                add(point[0], point[1]);
                lastX = point[0];
                lastY = point[1];
            }
            case SEG_QUADTO -> {
                type = SEG_LINETO;
                quad(lastX, lastY, point[0], point[1], point[2], point[3], 0);
                lastX = point[2];
                lastY = point[3];
            }
            case SEG_CUBICTO -> {
                type = SEG_LINETO;
                cubic(lastX, lastY, point[0], point[1], point[2], point[3], point[4], point[5],
                        0);
                lastX = point[4];
                lastY = point[5];
            }
            default -> type = SEG_CLOSE;
        }
    }

    private void quad(double x0, double y0, double cx, double cy, double x1, double y1,
                      int depth) {
        if (depth >= limit || Line2D.ptSegDistSq(x0, y0, x1, y1, cx, cy)
                <= flatness * flatness) {
            add(x1, y1);
            return;
        }
        double ax = (x0 + cx) / 2;
        double ay = (y0 + cy) / 2;
        double bx = (cx + x1) / 2;
        double by = (cy + y1) / 2;
        double mx = (ax + bx) / 2;
        double my = (ay + by) / 2;
        quad(x0, y0, ax, ay, mx, my, depth + 1);
        quad(mx, my, bx, by, x1, y1, depth + 1);
    }

    private void cubic(double x0, double y0, double c1x, double c1y, double c2x, double c2y,
                       double x1, double y1, int depth) {
        double limitSq = flatness * flatness;
        if (depth >= limit || Line2D.ptSegDistSq(x0, y0, x1, y1, c1x, c1y) <= limitSq
                && Line2D.ptSegDistSq(x0, y0, x1, y1, c2x, c2y) <= limitSq) {
            add(x1, y1);
            return;
        }
        double ax = (x0 + c1x) / 2;
        double ay = (y0 + c1y) / 2;
        double bx = (c1x + c2x) / 2;
        double by = (c1y + c2y) / 2;
        double cx = (c2x + x1) / 2;
        double cy = (c2y + y1) / 2;
        double abx = (ax + bx) / 2;
        double aby = (ay + by) / 2;
        double bcx = (bx + cx) / 2;
        double bcy = (by + cy) / 2;
        double mx = (abx + bcx) / 2;
        double my = (aby + bcy) / 2;
        cubic(x0, y0, ax, ay, abx, aby, mx, my, depth + 1);
        cubic(mx, my, bcx, bcy, cx, cy, x1, y1, depth + 1);
    }

    private void add(double x, double y) {
        if (count + 2 > lines.length) {
            lines = java.util.Arrays.copyOf(lines, lines.length * 2);
        }
        lines[count++] = x;
        lines[count++] = y;
    }
}
