package java.security.spec;

import java.math.BigInteger;

/** an rsa public key as its two numbers */
public class RSAPublicKeySpec implements KeySpec {

    private final BigInteger modulus;
    private final BigInteger exponent;

    public RSAPublicKeySpec(BigInteger modulus, BigInteger exponent) {
        this.modulus = modulus;
        this.exponent = exponent;
    }

    public BigInteger getModulus() {
        return modulus;
    }

    public BigInteger getPublicExponent() {
        return exponent;
    }
}
