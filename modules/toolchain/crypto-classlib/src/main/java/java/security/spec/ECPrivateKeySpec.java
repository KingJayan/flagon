package java.security.spec;

import java.math.BigInteger;
import java.util.Objects;

public class ECPrivateKeySpec implements KeySpec {

    private final BigInteger s;
    private final ECParameterSpec params;

    public ECPrivateKeySpec(BigInteger s, ECParameterSpec params) {
        this.s = Objects.requireNonNull(s, "s is null");
        this.params = Objects.requireNonNull(params, "params is null");
    }

    public BigInteger getS() {
        return s;
    }

    public ECParameterSpec getParams() {
        return params;
    }
}
