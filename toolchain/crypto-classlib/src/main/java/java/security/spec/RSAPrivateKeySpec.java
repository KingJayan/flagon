package java.security.spec;

import java.math.BigInteger;

/** an rsa private key as its two numbers */
public class RSAPrivateKeySpec implements KeySpec {

    private final BigInteger modulus;
    private final BigInteger exponent;

    public RSAPrivateKeySpec(BigInteger modulus, BigInteger exponent) {
        this.modulus = modulus;
        this.exponent = exponent;
    }

    public BigInteger getModulus() {
        return modulus;
    }

    public BigInteger getPrivateExponent() {
        return exponent;
    }
}
