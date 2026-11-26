package java.awt;

public final class AlphaComposite implements Composite {

    public static final int CLEAR = 1;
    public static final int SRC = 2;
    public static final int DST = 9;
    public static final int SRC_OVER = 3;
    public static final int DST_OVER = 4;
    public static final int SRC_IN = 5;
    public static final int DST_IN = 6;
    public static final int SRC_OUT = 7;
    public static final int DST_OUT = 8;
    public static final int SRC_ATOP = 10;
    public static final int DST_ATOP = 11;
    public static final int XOR = 12;

    public static final AlphaComposite Clear = new AlphaComposite(CLEAR, 1.0f);
    public static final AlphaComposite Src = new AlphaComposite(SRC, 1.0f);
    public static final AlphaComposite Dst = new AlphaComposite(DST, 1.0f);
    public static final AlphaComposite SrcOver = new AlphaComposite(SRC_OVER, 1.0f);
    public static final AlphaComposite DstOver = new AlphaComposite(DST_OVER, 1.0f);
    public static final AlphaComposite SrcIn = new AlphaComposite(SRC_IN, 1.0f);
    public static final AlphaComposite DstIn = new AlphaComposite(DST_IN, 1.0f);
    public static final AlphaComposite SrcOut = new AlphaComposite(SRC_OUT, 1.0f);
    public static final AlphaComposite DstOut = new AlphaComposite(DST_OUT, 1.0f);
    public static final AlphaComposite SrcAtop = new AlphaComposite(SRC_ATOP, 1.0f);
    public static final AlphaComposite DstAtop = new AlphaComposite(DST_ATOP, 1.0f);
    public static final AlphaComposite Xor = new AlphaComposite(XOR, 1.0f);

    private final int rule;
    private final float alpha;

    private AlphaComposite(int rule, float alpha) {
        if (rule < CLEAR || rule > XOR) {
            throw new IllegalArgumentException("unknown composite rule");
        }
        if (alpha < 0.0f || alpha > 1.0f) {
            throw new IllegalArgumentException("alpha value out of range");
        }
        this.rule = rule;
        this.alpha = alpha;
    }

    public static AlphaComposite getInstance(int rule) {
        return switch (rule) {
            case CLEAR -> Clear;
            case SRC -> Src;
            case DST -> Dst;
            case SRC_OVER -> SrcOver;
            case DST_OVER -> DstOver;
            case SRC_IN -> SrcIn;
            case DST_IN -> DstIn;
            case SRC_OUT -> SrcOut;
            case DST_OUT -> DstOut;
            case SRC_ATOP -> SrcAtop;
            case DST_ATOP -> DstAtop;
            case XOR -> Xor;
            default -> throw new IllegalArgumentException("unknown composite rule");
        };
    }

    public static AlphaComposite getInstance(int rule, float alpha) {
        return alpha == 1.0f ? getInstance(rule) : new AlphaComposite(rule, alpha);
    }

    public AlphaComposite derive(int rule) {
        return this.rule == rule ? this : getInstance(rule, alpha);
    }

    public AlphaComposite derive(float alpha) {
        return this.alpha == alpha ? this : getInstance(rule, alpha);
    }

    public int getRule() {
        return rule;
    }

    public float getAlpha() {
        return alpha;
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(alpha) * 31 + rule;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AlphaComposite composite && rule == composite.rule
                && alpha == composite.alpha;
    }
}
