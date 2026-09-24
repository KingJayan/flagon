package java.security;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Locale;

public class KeyFactory {

    private final String algorithm;

    protected KeyFactory(String algorithm) {
        this.algorithm = algorithm;
    }

    public static KeyFactory getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (!"RSA".equals(algorithm.toUpperCase(Locale.ROOT))) {
            throw new NoSuchAlgorithmException(algorithm + " keys are not read here; rsa are");
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
}
