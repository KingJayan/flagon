package java.awt.geom;

/** a path iterator over segments already worked out, moved by an optional transform */
final class Segments implements PathIterator {

    private final int[] types;
    private final double[] coords;
    private final int count;
    private final int winding;
    private final AffineTransform transform;
    private final double[] point = new double[6];
    private int index;
    private int at;

    Segments(int[] types, double[] coords, int count, int winding, AffineTransform transform) {
        this.types = types;
        this.coords = coords;
        this.count = count;
        this.winding = winding;
        this.transform = transform == null || transform.isIdentity() ? null : transform;
    }

    static int points(int type) {
        return switch (type) {
            case SEG_MOVETO, SEG_LINETO -> 1;
            case SEG_QUADTO -> 2;
            case SEG_CUBICTO -> 3;
            default -> 0;
        };
    }

    @Override
    public int getWindingRule() {
        return winding;
    }

    @Override
    public boolean isDone() {
        return index >= count;
    }

    @Override
    public void next() {
        if (index < count) {
            at += points(types[index]) * 2;
            index++;
        }
    }

    @Override
    public int currentSegment(double[] out) {
        int type = current();
        System.arraycopy(point, 0, out, 0, points(type) * 2);
        return type;
    }

    @Override
    public int currentSegment(float[] out) {
        int type = current();
        for (int i = 0; i < points(type) * 2; i++) {
            out[i] = (float) point[i];
        }
        return type;
    }

    private int current() {
        if (index >= count) {
            throw new java.util.NoSuchElementException("the path has no more segments");
        }
        int type = types[index];
        int length = points(type);
        System.arraycopy(coords, at, point, 0, length * 2);
        if (transform != null) {
            transform.transform(point, 0, point, 0, length);
        }
        return type;
    }
}
