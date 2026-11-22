package java.security.spec;

import java.math.BigInteger;
import java.util.Objects;

/** a point in affine coordinates, or the point at infinity */
public class ECPoint {

    public static final ECPoint POINT_INFINITY = new ECPoint();

    private final BigInteger x;
    private final BigInteger y;

    private ECPoint() {
        this.x = null;
        this.y = null;
    }

    public ECPoint(BigInteger x, BigInteger y) {
        this.x = Objects.requireNonNull(x, "affine coordinate x is null");
        this.y = Objects.requireNonNull(y, "affine coordinate y is null");
    }

    public BigInteger getAffineX() {
        return x;
    }

    public BigInteger getAffineY() {
        return y;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (this == POINT_INFINITY || other == POINT_INFINITY) {
            return false;
        }
        return other instanceof ECPoint that && x.equals(that.x) && y.equals(that.y);
    }

    @Override
    public int hashCode() {
        return this == POINT_INFINITY ? 0 : (x.hashCode() << 5) + y.hashCode();
    }
}
