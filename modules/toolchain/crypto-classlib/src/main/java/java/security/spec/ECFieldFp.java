package java.security.spec;

import java.math.BigInteger;

/** the field of integers modulo an odd prime */
public class ECFieldFp implements ECField {

    private final BigInteger p;

    public ECFieldFp(BigInteger p) {
        if (p.signum() != 1) {
            throw new IllegalArgumentException("p is not positive");
        }
        this.p = p;
    }

    @Override
    public int getFieldSize() {
        return p.bitLength();
    }

    public BigInteger getP() {
        return p;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ECFieldFp that && p.equals(that.p);
    }

    @Override
    public int hashCode() {
        return p.hashCode();
    }
}
