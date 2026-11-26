package java.security.spec;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

/** y^2 = x^3 + ax + b over a prime field */
public class EllipticCurve {

    private final ECField field;
    private final BigInteger a;
    private final BigInteger b;
    private final byte[] seed;

    public EllipticCurve(ECField field, BigInteger a, BigInteger b) {
        this(field, a, b, null);
    }

    public EllipticCurve(ECField field, BigInteger a, BigInteger b, byte[] seed) {
        this.field = Objects.requireNonNull(field, "field is null");
        this.a = Objects.requireNonNull(a, "first coefficient is null");
        this.b = Objects.requireNonNull(b, "second coefficient is null");
        if (field instanceof ECFieldFp prime) {
            BigInteger p = prime.getP();
            if (a.signum() < 0 || a.compareTo(p) >= 0) {
                throw new IllegalArgumentException("a is not in field");
            }
            if (b.signum() < 0 || b.compareTo(p) >= 0) {
                throw new IllegalArgumentException("b is not in field");
            }
        }
        this.seed = seed == null ? null : seed.clone();
    }

    public ECField getField() {
        return field;
    }

    public BigInteger getA() {
        return a;
    }

    public BigInteger getB() {
        return b;
    }

    public byte[] getSeed() {
        return seed == null ? null : seed.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EllipticCurve that && field.equals(that.field)
                && a.equals(that.a) && b.equals(that.b);
    }

    @Override
    public int hashCode() {
        return field.hashCode() << 6 + (a.hashCode() << 4) + (b.hashCode() << 2);
    }

    @Override
    public String toString() {
        return "EllipticCurve[" + field + ", " + a + ", " + b + ", " + Arrays.toString(seed) + "]";
    }
}
