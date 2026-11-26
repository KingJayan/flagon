package java.awt;

import java.util.Arrays;

public class BasicStroke implements Stroke {

    public static final int JOIN_MITER = 0;
    public static final int JOIN_ROUND = 1;
    public static final int JOIN_BEVEL = 2;
    public static final int CAP_BUTT = 0;
    public static final int CAP_ROUND = 1;
    public static final int CAP_SQUARE = 2;

    private final float width;
    private final int cap;
    private final int join;
    private final float miterLimit;
    private final float[] dash;
    private final float dashPhase;

    public BasicStroke() {
        this(1.0f, CAP_SQUARE, JOIN_MITER, 10.0f, null, 0.0f);
    }

    public BasicStroke(float width) {
        this(width, CAP_SQUARE, JOIN_MITER, 10.0f, null, 0.0f);
    }

    public BasicStroke(float width, int cap, int join) {
        this(width, cap, join, 10.0f, null, 0.0f);
    }

    public BasicStroke(float width, int cap, int join, float miterLimit) {
        this(width, cap, join, miterLimit, null, 0.0f);
    }

    public BasicStroke(float width, int cap, int join, float miterLimit, float[] dash,
                       float dashPhase) {
        if (width < 0.0f) {
            throw new IllegalArgumentException("negative width");
        }
        if (cap != CAP_BUTT && cap != CAP_ROUND && cap != CAP_SQUARE) {
            throw new IllegalArgumentException("illegal end cap value");
        }
        if (join == JOIN_MITER) {
            if (miterLimit < 1.0f) {
                throw new IllegalArgumentException("miter limit < 1");
            }
        } else if (join != JOIN_ROUND && join != JOIN_BEVEL) {
            throw new IllegalArgumentException("illegal line join value");
        }
        if (dash != null) {
            if (dashPhase < 0.0f) {
                throw new IllegalArgumentException("negative dash phase");
            }
            boolean allZero = true;
            for (float length : dash) {
                if (length > 0.0f) {
                    allZero = false;
                } else if (length < 0.0f) {
                    throw new IllegalArgumentException("negative dash length");
                }
            }
            if (allZero) {
                throw new IllegalArgumentException("dash lengths all zero");
            }
        }
        this.width = width;
        this.cap = cap;
        this.join = join;
        this.miterLimit = miterLimit;
        this.dash = dash == null ? null : dash.clone();
        this.dashPhase = dashPhase;
    }

    public float getLineWidth() {
        return width;
    }

    public int getEndCap() {
        return cap;
    }

    public int getLineJoin() {
        return join;
    }

    public float getMiterLimit() {
        return miterLimit;
    }

    public float[] getDashArray() {
        return dash == null ? null : dash.clone();
    }

    public float getDashPhase() {
        return dashPhase;
    }

    @Override
    public int hashCode() {
        int hash = Float.floatToIntBits(width);
        hash = hash * 31 + join;
        hash = hash * 31 + cap;
        hash = hash * 31 + Float.floatToIntBits(miterLimit);
        if (dash != null) {
            hash = hash * 31 + Float.floatToIntBits(dashPhase);
            for (float length : dash) {
                hash = hash * 31 + Float.floatToIntBits(length);
            }
        }
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BasicStroke stroke)) {
            return false;
        }
        if (width != stroke.width || join != stroke.join || cap != stroke.cap
                || miterLimit != stroke.miterLimit) {
            return false;
        }
        if (dash == null) {
            return stroke.dash == null;
        }
        return dashPhase == stroke.dashPhase && Arrays.equals(dash, stroke.dash);
    }
}
