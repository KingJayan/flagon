package javax.crypto;

import javax.crypto.spec.SecretKeySpec;
import java.security.Ec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Locale;

/** ecdh on the nist curves */
public class KeyAgreement {

    private final String algorithm;
    private Ec.Private mine;
    private byte[] secret;

    private KeyAgreement(String algorithm) {
        this.algorithm = algorithm;
    }

    public static final KeyAgreement getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        if (!"ECDH".equals(algorithm.toUpperCase(Locale.ROOT))) {
            throw new NoSuchAlgorithmException(algorithm + " is not written here; ECDH is");
        }
        return new KeyAgreement(algorithm);
    }

    public static final KeyAgreement getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException {
        return getInstance(algorithm);
    }

    public final String getAlgorithm() {
        return algorithm;
    }

    public final void init(Key key) throws InvalidKeyException {
        if (!(key instanceof Ec.Private found)) {
            throw new InvalidKeyException("ECDH takes an ec private key");
        }
        mine = found;
        secret = null;
    }

    public final void init(Key key, SecureRandom random) throws InvalidKeyException {
        init(key);
    }

    public final void init(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(key, params, null);
    }

    public final void init(Key key, AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params != null) {
            throw new InvalidAlgorithmParameterException("Parameters not supported");
        }
        init(key);
    }

    public final Key doPhase(Key key, boolean lastPhase) throws InvalidKeyException {
        if (mine == null) {
            throw new IllegalStateException("Not initialized");
        }
        if (secret != null) {
            throw new IllegalStateException("Phase already executed");
        }
        if (!lastPhase) {
            throw new IllegalStateException(
                    "Only two party agreement supported, lastPhase must be true");
        }
        if (!(key instanceof ECPublicKey theirs)) {
            throw new InvalidKeyException("Key must be an instance of PublicKey");
        }
        if (!theirs.getParams().getCurve().equals(mine.getParams().getCurve())
                || !Ec.isOnCurve(Ec.of(mine.getParams()), theirs.getW())) {
            throw new InvalidKeyException("the public key is not on the private key's curve");
        }
        secret = Ec.agree(mine, theirs);
        return null;
    }

    public final byte[] generateSecret() {
        if (secret == null) {
            throw new IllegalStateException("Key agreement has not been completed yet");
        }
        byte[] made = secret;
        secret = null;
        return made;
    }

    public final int generateSecret(byte[] sharedSecret, int offset) throws ShortBufferException {
        if (secret == null) {
            throw new IllegalStateException("Key agreement has not been completed yet");
        }
        if (sharedSecret.length - offset < secret.length) {
            throw new ShortBufferException("Need " + secret.length
                    + " bytes, only " + (sharedSecret.length - offset) + " available");
        }
        byte[] made = generateSecret();
        System.arraycopy(made, 0, sharedSecret, offset, made.length);
        return made.length;
    }

    public final SecretKey generateSecret(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NoSuchAlgorithmException("null algorithm");
        }
        if (!algorithm.equalsIgnoreCase("TlsPremasterSecret")) {
            throw new NoSuchAlgorithmException("Unsupported secret key algorithm: " + algorithm);
        }
        return new SecretKeySpec(generateSecret(), "TlsPremasterSecret");
    }
}
