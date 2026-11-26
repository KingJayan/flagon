package java.security.spec;

import java.util.Objects;

public class ECPublicKeySpec implements KeySpec {

    private final ECPoint w;
    private final ECParameterSpec params;

    public ECPublicKeySpec(ECPoint w, ECParameterSpec params) {
        this.w = Objects.requireNonNull(w, "w is null");
        this.params = Objects.requireNonNull(params, "params is null");
        if (w == ECPoint.POINT_INFINITY) {
            throw new IllegalArgumentException("w is ECPoint.POINT_INFINITY");
        }
    }

    public ECPoint getW() {
        return w;
    }

    public ECParameterSpec getParams() {
        return params;
    }
}
