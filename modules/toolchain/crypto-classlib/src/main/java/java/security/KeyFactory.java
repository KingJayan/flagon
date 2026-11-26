package java.security;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Locale;

public class KeyFactory {

    private final String algorithm;
    private final boolean ec;

    protected KeyFactory(String algorithm) {
        this.algorithm = algorithm;
        this.ec = "EC".equals(algorithm.toUpperCase(Locale.ROOT));
    }

    public static KeyFactory getInstance(String algorithm) throws NoSuchAlgorithmException {
        String asked = algorithm.toUpperCase(Locale.ROOT);
        if (!"RSA".equals(asked) && !"EC".equals(asked)) {
            throw new NoSuchAlgorithmException(algorithm
                    + " keys are not read here; rsa and ec are");
        }
        return new KeyFactory(algorithm);
    }

    public static KeyFactory getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException {
        return getInstance(algorithm);
    }

    public final String getAlgorithm() {
        return algorithm;
    }

    public final PublicKey generatePublic(KeySpec spec) throws InvalidKeySpecException {
        if (ec) {
            return ecPublic(spec);
        }
        try {
            if (spec instanceof X509EncodedKeySpec x509) {
                return Rsa.readPublic(x509.getEncoded());
            }
            if (spec instanceof RSAPublicKeySpec numbers) {
                return new Rsa.Public(numbers.getModulus(), numbers.getPublicExponent());
            }
        } catch (RuntimeException unreadable) {
            throw new InvalidKeySpecException("these bytes are not an rsa public key",
                    unreadable);
        }
        throw new InvalidKeySpecException(
                "a public key is read from X.509 bytes or its two numbers here");
    }

    public final PrivateKey generatePrivate(KeySpec spec) throws InvalidKeySpecException {
        if (ec) {
            return ecPrivate(spec);
        }
        try {
            if (spec instanceof PKCS8EncodedKeySpec pkcs8) {
                return Rsa.readPrivate(pkcs8.getEncoded());
            }
            if (spec instanceof RSAPrivateKeySpec numbers) {
                return new Rsa.Private(numbers.getModulus(), numbers.getPrivateExponent());
            }
        } catch (RuntimeException unreadable) {
            throw new InvalidKeySpecException("these bytes are not an rsa private key",
                    unreadable);
        }
        throw new InvalidKeySpecException(
                "a private key is read from PKCS#8 bytes or its two numbers here");
    }

    @SuppressWarnings("unchecked")
    public final <T extends KeySpec> T getKeySpec(Key key, Class<T> want)
            throws InvalidKeySpecException {
        if (want == X509EncodedKeySpec.class && key instanceof PublicKey) {
            return (T) new X509EncodedKeySpec(key.getEncoded());
        }
        if (want == PKCS8EncodedKeySpec.class && key instanceof PrivateKey) {
            return (T) new PKCS8EncodedKeySpec(key.getEncoded());
        }
        if (want == ECPublicKeySpec.class && key instanceof ECPublicKey found) {
            return (T) new ECPublicKeySpec(found.getW(), found.getParams());
        }
        if (want == ECPrivateKeySpec.class && key instanceof ECPrivateKey found) {
            return (T) new ECPrivateKeySpec(found.getS(), found.getParams());
        }
        if (want == RSAPublicKeySpec.class && key instanceof Rsa.Public rsa) {
            return (T) new RSAPublicKeySpec(rsa.modulus(), rsa.exponent());
        }
        if (want == RSAPrivateKeySpec.class && key instanceof Rsa.Private rsa) {
            return (T) new RSAPrivateKeySpec(rsa.modulus(), rsa.exponent());
        }
        throw new InvalidKeySpecException(
                "this key does not write itself as " + want.getName());
    }

    public final Key translateKey(Key key) {
        return key;
    }

    private static PublicKey ecPublic(KeySpec spec) throws InvalidKeySpecException {
        try {
            if (spec instanceof X509EncodedKeySpec x509) {
                return Ec.readPublic(x509.getEncoded());
            }
            if (spec instanceof ECPublicKeySpec point) {
                return new Ec.Public(curveOf(point.getParams()), point.getW());
            }
        } catch (RuntimeException unreadable) {
            throw new InvalidKeySpecException(unreadable.getMessage(), unreadable);
        }
        throw new InvalidKeySpecException(
                "an ec public key is read from X.509 bytes or an ECPublicKeySpec here");
    }

    private static PrivateKey ecPrivate(KeySpec spec) throws InvalidKeySpecException {
        try {
            if (spec instanceof PKCS8EncodedKeySpec pkcs8) {
                return Ec.readPrivate(pkcs8.getEncoded());
            }
            if (spec instanceof ECPrivateKeySpec secret) {
                return new Ec.Private(curveOf(secret.getParams()), secret.getS());
            }
        } catch (RuntimeException unreadable) {
            throw new InvalidKeySpecException(unreadable.getMessage(), unreadable);
        }
        throw new InvalidKeySpecException(
                "an ec private key is read from PKCS#8 bytes or an ECPrivateKeySpec here");
    }

    private static Ec.Curve curveOf(java.security.spec.ECParameterSpec params) {
        Ec.Curve curve = Ec.of(params);
        if (curve == null) {
            throw new IllegalArgumentException("that curve is not written here;"
                    + " secp256r1, secp384r1 and secp521r1 are");
        }
        return curve;
    }
}
