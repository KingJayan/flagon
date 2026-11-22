package java.security.spec;

import java.math.BigInteger;
import java.util.Objects;

public class ECParameterSpec implements AlgorithmParameterSpec {

    private final EllipticCurve curve;
    private final ECPoint generator;
    private final BigInteger order;
    private final int cofactor;

    public ECParameterSpec(EllipticCurve curve, ECPoint generator, BigInteger order,
                           int cofactor) {
        this.curve = Objects.requireNonNull(curve, "curve is null");
        this.generator = Objects.requireNonNull(generator, "g is null");
        this.order = Objects.requireNonNull(order, "n is null");
        if (order.signum() != 1) {
            throw new IllegalArgumentException("n is not positive");
        }
        if (cofactor <= 0) {
            throw new IllegalArgumentException("h is not positive");
        }
        this.cofactor = cofactor;
    }

    public EllipticCurve getCurve() {
        return curve;
    }

    public ECPoint getGenerator() {
        return generator;
    }

    public BigInteger getOrder() {
        return order;
    }

    public int getCofactor() {
        return cofactor;
    }
}
