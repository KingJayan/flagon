package java.awt.geom;

import java.awt.Shape;

public class AffineTransform implements Cloneable {

    public static final int TYPE_IDENTITY = 0;
    public static final int TYPE_TRANSLATION = 1;
    public static final int TYPE_UNIFORM_SCALE = 2;
    public static final int TYPE_GENERAL_SCALE = 4;
    public static final int TYPE_MASK_SCALE = TYPE_UNIFORM_SCALE | TYPE_GENERAL_SCALE;
    public static final int TYPE_FLIP = 64;
    public static final int TYPE_QUADRANT_ROTATION = 8;
    public static final int TYPE_GENERAL_ROTATION = 16;
    public static final int TYPE_MASK_ROTATION = TYPE_QUADRANT_ROTATION | TYPE_GENERAL_ROTATION;
    public static final int TYPE_GENERAL_TRANSFORM = 32;

    private double m00;
    private double m10;
    private double m01;
    private double m11;
    private double m02;
    private double m12;

    public AffineTransform() {
        m00 = 1.0;
        m11 = 1.0;
    }

    public AffineTransform(AffineTransform other) {
        setTransform(other);
    }

    public AffineTransform(float m00, float m10, float m01, float m11, float m02, float m12) {
        setTransform(m00, m10, m01, m11, m02, m12);
    }

    public AffineTransform(float[] matrix) {
        setTransform(matrix[0], matrix[1], matrix[2], matrix[3],
                matrix.length > 5 ? matrix[4] : 0, matrix.length > 5 ? matrix[5] : 0);
    }

    public AffineTransform(double m00, double m10, double m01, double m11, double m02,
                           double m12) {
        setTransform(m00, m10, m01, m11, m02, m12);
    }

    public AffineTransform(double[] matrix) {
        setTransform(matrix[0], matrix[1], matrix[2], matrix[3],
                matrix.length > 5 ? matrix[4] : 0, matrix.length > 5 ? matrix[5] : 0);
    }

    public static AffineTransform getTranslateInstance(double tx, double ty) {
        AffineTransform made = new AffineTransform();
        made.setToTranslation(tx, ty);
        return made;
    }

    public static AffineTransform getRotateInstance(double theta) {
        AffineTransform made = new AffineTransform();
        made.setToRotation(theta);
        return made;
    }

    public static AffineTransform getRotateInstance(double theta, double anchorX,
                                                    double anchorY) {
        AffineTransform made = new AffineTransform();
        made.setToRotation(theta, anchorX, anchorY);
        return made;
    }

    public static AffineTransform getRotateInstance(double vectorX, double vectorY) {
        AffineTransform made = new AffineTransform();
        made.setToRotation(vectorX, vectorY);
        return made;
    }

    public static AffineTransform getRotateInstance(double vectorX, double vectorY,
                                                    double anchorX, double anchorY) {
        AffineTransform made = new AffineTransform();
        made.setToRotation(vectorX, vectorY, anchorX, anchorY);
        return made;
    }

    public static AffineTransform getQuadrantRotateInstance(int quadrants) {
        AffineTransform made = new AffineTransform();
        made.setToQuadrantRotation(quadrants);
        return made;
    }

    public static AffineTransform getQuadrantRotateInstance(int quadrants, double anchorX,
                                                            double anchorY) {
        AffineTransform made = new AffineTransform();
        made.setToQuadrantRotation(quadrants, anchorX, anchorY);
        return made;
    }

    public static AffineTransform getScaleInstance(double sx, double sy) {
        AffineTransform made = new AffineTransform();
        made.setToScale(sx, sy);
        return made;
    }

    public static AffineTransform getShearInstance(double shx, double shy) {
        AffineTransform made = new AffineTransform();
        made.setToShear(shx, shy);
        return made;
    }

    public int getType() {
        if (m01 == 0.0 && m10 == 0.0) {
            return translation() | axisType(m00, m11);
        }
        if (m00 == 0.0 && m11 == 0.0) {
            return translation() | quadrantType(m01, m10);
        }
        if (m00 * m01 + m10 * m11 != 0.0) {
            return TYPE_GENERAL_TRANSFORM;
        }
        int type = translation() | TYPE_GENERAL_ROTATION;
        boolean determinantOne = m00 * m11 - m01 * m10 == 1.0;
        if ((m00 >= 0.0) == (m11 >= 0.0)) {
            if (m00 != m11 || m01 != -m10) {
                return type | TYPE_GENERAL_SCALE;
            }
            return determinantOne ? type : type | TYPE_UNIFORM_SCALE;
        }
        type |= TYPE_FLIP;
        if (m00 != -m11 || m01 != m10) {
            return type | TYPE_GENERAL_SCALE;
        }
        return determinantOne ? type : type | TYPE_UNIFORM_SCALE;
    }

    private int translation() {
        return m02 != 0.0 || m12 != 0.0 ? TYPE_TRANSLATION : TYPE_IDENTITY;
    }

    private static int axisType(double sx, double sy) {
        if ((sx >= 0.0) == (sy >= 0.0)) {
            int type = sx >= 0.0 ? 0 : TYPE_QUADRANT_ROTATION;
            if (sx != sy) {
                return type | TYPE_GENERAL_SCALE;
            }
            return sx == 1.0 || sx == -1.0 ? type : type | TYPE_UNIFORM_SCALE;
        }
        if (sx != -sy) {
            return TYPE_FLIP | TYPE_GENERAL_SCALE;
        }
        return sx == 1.0 || sx == -1.0 ? TYPE_FLIP : TYPE_FLIP | TYPE_UNIFORM_SCALE;
    }

    private static int quadrantType(double shx, double shy) {
        if ((shx >= 0.0) != (shy >= 0.0)) {
            if (shx != -shy) {
                return TYPE_QUADRANT_ROTATION | TYPE_GENERAL_SCALE;
            }
            return shx == 1.0 || shx == -1.0 ? TYPE_QUADRANT_ROTATION
                    : TYPE_QUADRANT_ROTATION | TYPE_UNIFORM_SCALE;
        }
        return TYPE_QUADRANT_ROTATION | TYPE_FLIP
                | (shx == shy ? TYPE_UNIFORM_SCALE : TYPE_GENERAL_SCALE);
    }

    public double getDeterminant() {
        return m00 * m11 - m01 * m10;
    }

    public void getMatrix(double[] matrix) {
        matrix[0] = m00;
        matrix[1] = m10;
        matrix[2] = m01;
        matrix[3] = m11;
        if (matrix.length > 5) {
            matrix[4] = m02;
            matrix[5] = m12;
        }
    }

    public double getScaleX() {
        return m00;
    }

    public double getScaleY() {
        return m11;
    }

    public double getShearX() {
        return m01;
    }

    public double getShearY() {
        return m10;
    }

    public double getTranslateX() {
        return m02;
    }

    public double getTranslateY() {
        return m12;
    }

    public void translate(double tx, double ty) {
        m02 = tx * m00 + ty * m01 + m02;
        m12 = tx * m10 + ty * m11 + m12;
    }

    public void rotate(double theta) {
        double sin = Math.sin(theta);
        if (sin == 1.0) {
            quadrantRotate(1);
        } else if (sin == -1.0) {
            quadrantRotate(3);
        } else {
            double cos = Math.cos(theta);
            if (cos == -1.0) {
                quadrantRotate(2);
            } else if (cos != 1.0) {
                turn(sin, cos);
            }
        }
    }

    private void turn(double sin, double cos) {
        double a = m00;
        double b = m01;
        m00 = cos * a + sin * b;
        m01 = -sin * a + cos * b;
        a = m10;
        b = m11;
        m10 = cos * a + sin * b;
        m11 = -sin * a + cos * b;
    }

    public void rotate(double theta, double anchorX, double anchorY) {
        translate(anchorX, anchorY);
        rotate(theta);
        translate(-anchorX, -anchorY);
    }

    public void rotate(double vectorX, double vectorY) {
        if (vectorY == 0.0) {
            if (vectorX < 0.0) {
                quadrantRotate(2);
            }
        } else if (vectorX == 0.0) {
            quadrantRotate(vectorY > 0.0 ? 1 : 3);
        } else {
            double length = Math.sqrt(vectorX * vectorX + vectorY * vectorY);
            turn(vectorY / length, vectorX / length);
        }
    }

    public void rotate(double vectorX, double vectorY, double anchorX, double anchorY) {
        translate(anchorX, anchorY);
        rotate(vectorX, vectorY);
        translate(-anchorX, -anchorY);
    }

    public void quadrantRotate(int quadrants) {
        switch (quadrants & 3) {
            case 1 -> {
                double a = m00;
                m00 = m01;
                m01 = -a;
                a = m10;
                m10 = m11;
                m11 = -a;
            }
            case 2 -> {
                m00 = -m00;
                m11 = -m11;
                if (m01 != 0.0 || m10 != 0.0) {
                    m01 = -m01;
                    m10 = -m10;
                }
            }
            case 3 -> {
                double a = m00;
                m00 = -m01;
                m01 = a;
                a = m10;
                m10 = -m11;
                m11 = a;
            }
            default -> {
            }
        }
    }

    public void quadrantRotate(int quadrants, double anchorX, double anchorY) {
        translate(anchorX, anchorY);
        quadrantRotate(quadrants);
        translate(-anchorX, -anchorY);
    }

    public void scale(double sx, double sy) {
        m00 *= sx;
        m10 *= sx;
        m01 *= sy;
        m11 *= sy;
    }

    public void shear(double shx, double shy) {
        double a = m00;
        double b = m01;
        m00 = a + b * shy;
        m01 = a * shx + b;
        a = m10;
        b = m11;
        m10 = a + b * shy;
        m11 = a * shx + b;
    }

    public void setToIdentity() {
        setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0);
    }

    public void setToTranslation(double tx, double ty) {
        setTransform(1.0, 0.0, 0.0, 1.0, tx, ty);
    }

    public void setToRotation(double theta) {
        double sin = Math.sin(theta);
        double cos = Math.cos(theta);
        if (sin == 1.0 || sin == -1.0) {
            cos = 0.0;
        } else if (cos == 1.0 || cos == -1.0) {
            sin = 0.0;
        }
        setTransform(cos, sin, -sin, cos, 0.0, 0.0);
    }

    public void setToRotation(double theta, double anchorX, double anchorY) {
        setToRotation(theta);
        anchor(anchorX, anchorY);
    }

    private void anchor(double anchorX, double anchorY) {
        double sin = m10;
        double oneLessCos = 1.0 - m00;
        m02 = anchorX * oneLessCos + anchorY * sin;
        m12 = anchorY * oneLessCos - anchorX * sin;
    }

    public void setToRotation(double vectorX, double vectorY) {
        double sin;
        double cos;
        if (vectorY == 0) {
            sin = 0.0;
            cos = vectorX < 0.0 ? -1.0 : 1.0;
        } else if (vectorX == 0) {
            cos = 0.0;
            sin = vectorY > 0.0 ? 1.0 : -1.0;
        } else {
            double length = Math.sqrt(vectorX * vectorX + vectorY * vectorY);
            cos = vectorX / length;
            sin = vectorY / length;
        }
        setTransform(cos, sin, -sin, cos, 0.0, 0.0);
    }

    public void setToRotation(double vectorX, double vectorY, double anchorX, double anchorY) {
        setToRotation(vectorX, vectorY);
        anchor(anchorX, anchorY);
    }

    public void setToQuadrantRotation(int quadrants) {
        switch (quadrants & 3) {
            case 1 -> setTransform(0.0, 1.0, -1.0, 0.0, 0.0, 0.0);
            case 2 -> setTransform(-1.0, 0.0, 0.0, -1.0, 0.0, 0.0);
            case 3 -> setTransform(0.0, -1.0, 1.0, 0.0, 0.0, 0.0);
            default -> setToIdentity();
        }
    }

    public void setToQuadrantRotation(int quadrants, double anchorX, double anchorY) {
        setToQuadrantRotation(quadrants);
        anchor(anchorX, anchorY);
    }

    public void setToScale(double sx, double sy) {
        setTransform(sx, 0.0, 0.0, sy, 0.0, 0.0);
    }

    public void setToShear(double shx, double shy) {
        setTransform(1.0, shy, shx, 1.0, 0.0, 0.0);
    }

    public void setTransform(AffineTransform other) {
        setTransform(other.m00, other.m10, other.m01, other.m11, other.m02, other.m12);
    }

    public void setTransform(double m00, double m10, double m01, double m11, double m02,
                             double m12) {
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
    }

    public void concatenate(AffineTransform other) {
        double a = m00;
        double b = m01;
        m00 = a * other.m00 + b * other.m10;
        m01 = a * other.m01 + b * other.m11;
        m02 = a * other.m02 + b * other.m12 + m02;
        a = m10;
        b = m11;
        m10 = a * other.m00 + b * other.m10;
        m11 = a * other.m01 + b * other.m11;
        m12 = a * other.m02 + b * other.m12 + m12;
    }

    public void preConcatenate(AffineTransform other) {
        double a = m00;
        double b = m10;
        m00 = other.m00 * a + other.m01 * b;
        m10 = other.m10 * a + other.m11 * b;
        a = m01;
        b = m11;
        m01 = other.m00 * a + other.m01 * b;
        m11 = other.m10 * a + other.m11 * b;
        a = m02;
        b = m12;
        m02 = other.m00 * a + other.m01 * b + other.m02;
        m12 = other.m10 * a + other.m11 * b + other.m12;
    }

    public AffineTransform createInverse() throws NoninvertibleTransformException {
        AffineTransform inverse = new AffineTransform(this);
        inverse.invert();
        return inverse;
    }

    public void invert() throws NoninvertibleTransformException {
        if (m01 == 0.0 && m10 == 0.0) {
            if (m00 == 0.0 || m11 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0.0");
            }
            double sx = m00;
            double sy = m11;
            setTransform(1.0 / sx, 0.0, 0.0, 1.0 / sy, m02 == 0.0 ? 0.0 : -m02 / sx,
                    m12 == 0.0 ? 0.0 : -m12 / sy);
            return;
        }
        double determinant = getDeterminant();
        if (Math.abs(determinant) <= Double.MIN_VALUE) {
            throw new NoninvertibleTransformException("Determinant is " + determinant);
        }
        setTransform(m11 / determinant, -m10 / determinant, -m01 / determinant,
                m00 / determinant, (m01 * m12 - m11 * m02) / determinant,
                (m10 * m02 - m00 * m12) / determinant);
    }

    public Point2D transform(Point2D from, Point2D to) {
        if (to == null) {
            to = from instanceof Point2D.Double ? new Point2D.Double() : new Point2D.Float();
        }
        double x = from.getX();
        double y = from.getY();
        to.setLocation(x * m00 + y * m01 + m02, x * m10 + y * m11 + m12);
        return to;
    }

    public void transform(Point2D[] from, int fromOffset, Point2D[] to, int toOffset,
                          int count) {
        for (int i = 0; i < count; i++) {
            Point2D point = from[fromOffset + i];
            Point2D into = to[toOffset + i];
            if (into == null) {
                into = point instanceof Point2D.Double ? new Point2D.Double()
                        : new Point2D.Float();
                to[toOffset + i] = into;
            }
            double x = point.getX();
            double y = point.getY();
            into.setLocation(x * m00 + y * m01 + m02, x * m10 + y * m11 + m12);
        }
    }

    public void transform(double[] from, int fromOffset, double[] to, int toOffset,
                          int count) {
        double[] source = from == to ? java.util.Arrays.copyOfRange(from, fromOffset,
                fromOffset + count * 2) : from;
        int at = from == to ? 0 : fromOffset;
        for (int i = 0; i < count; i++) {
            double x = source[at++];
            double y = source[at++];
            to[toOffset++] = x * m00 + y * m01 + m02;
            to[toOffset++] = x * m10 + y * m11 + m12;
        }
    }

    public void transform(float[] from, int fromOffset, float[] to, int toOffset, int count) {
        float[] source = from == to ? java.util.Arrays.copyOfRange(from, fromOffset,
                fromOffset + count * 2) : from;
        int at = from == to ? 0 : fromOffset;
        for (int i = 0; i < count; i++) {
            double x = source[at++];
            double y = source[at++];
            to[toOffset++] = (float) (x * m00 + y * m01 + m02);
            to[toOffset++] = (float) (x * m10 + y * m11 + m12);
        }
    }

    public void transform(float[] from, int fromOffset, double[] to, int toOffset, int count) {
        for (int i = 0; i < count; i++) {
            double x = from[fromOffset++];
            double y = from[fromOffset++];
            to[toOffset++] = x * m00 + y * m01 + m02;
            to[toOffset++] = x * m10 + y * m11 + m12;
        }
    }

    public void transform(double[] from, int fromOffset, float[] to, int toOffset, int count) {
        for (int i = 0; i < count; i++) {
            double x = from[fromOffset++];
            double y = from[fromOffset++];
            to[toOffset++] = (float) (x * m00 + y * m01 + m02);
            to[toOffset++] = (float) (x * m10 + y * m11 + m12);
        }
    }

    public Point2D inverseTransform(Point2D from, Point2D to)
            throws NoninvertibleTransformException {
        return createInverse().transform(from, to);
    }

    public void inverseTransform(double[] from, int fromOffset, double[] to, int toOffset,
                                 int count) throws NoninvertibleTransformException {
        createInverse().transform(from, fromOffset, to, toOffset, count);
    }

    public Point2D deltaTransform(Point2D from, Point2D to) {
        if (to == null) {
            to = from instanceof Point2D.Double ? new Point2D.Double() : new Point2D.Float();
        }
        double x = from.getX();
        double y = from.getY();
        to.setLocation(x * m00 + y * m01, x * m10 + y * m11);
        return to;
    }

    public void deltaTransform(double[] from, int fromOffset, double[] to, int toOffset,
                               int count) {
        double[] source = from == to ? java.util.Arrays.copyOfRange(from, fromOffset,
                fromOffset + count * 2) : from;
        int at = from == to ? 0 : fromOffset;
        for (int i = 0; i < count; i++) {
            double x = source[at++];
            double y = source[at++];
            to[toOffset++] = x * m00 + y * m01;
            to[toOffset++] = x * m10 + y * m11;
        }
    }

    public Shape createTransformedShape(Shape shape) {
        return shape == null ? null : new Path2D.Double(shape, this);
    }

    public boolean isIdentity() {
        return m00 == 1.0 && m11 == 1.0 && m01 == 0.0 && m10 == 0.0 && m02 == 0.0
                && m12 == 0.0;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException impossible) {
            throw new InternalError(impossible);
        }
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(m00);
        bits = bits * 31 + Double.doubleToLongBits(m01);
        bits = bits * 31 + Double.doubleToLongBits(m02);
        bits = bits * 31 + Double.doubleToLongBits(m10);
        bits = bits * 31 + Double.doubleToLongBits(m11);
        bits = bits * 31 + Double.doubleToLongBits(m12);
        return (int) bits ^ (int) (bits >> 32);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AffineTransform that && m00 == that.m00 && m01 == that.m01
                && m02 == that.m02 && m10 == that.m10 && m11 == that.m11 && m12 == that.m12;
    }

    @Override
    public String toString() {
        return "AffineTransform[[" + round(m00) + ", " + round(m01) + ", " + round(m02)
                + "], [" + round(m10) + ", " + round(m11) + ", " + round(m12) + "]]";
    }

    private static double round(double value) {
        return Math.rint(value * 1E15) / 1E15;
    }
}
