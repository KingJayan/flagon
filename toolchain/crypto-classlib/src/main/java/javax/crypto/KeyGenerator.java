package javax.crypto;

import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

public class KeyGenerator {

    private final String algorithm;

    private int bits = 128;
    private SecureRandom random = new SecureRandom();

    private KeyGenerator(String algorithm) {
        this.algorithm = algorithm;
    }

    public static KeyGenerator getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (!"AES".equals(algorithm.toUpperCase(Locale.ROOT))) {
            throw new NoSuchAlgorithmException(
                    algorithm + " keys are not made here; aes are");
        }
        return new KeyGenerator(algorithm);
    }

    public static KeyGenerator getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException {
        return getInstance(algorithm);
    }

    public final String getAlgorithm() {
        return algorithm;
    }

    public final void init(int size) {
        if (size != 128 && size != 192 && size != 256) {
            throw new IllegalArgumentException(
                    "an aes key is 128, 192 or 256 bits, got " + size);
        }
        this.bits = size;
    }

    public final void init(int size, SecureRandom source) {
        init(size);
        this.random = source;
    }

    public final void init(SecureRandom source) {
        this.random = source;
    }

    public final SecretKey generateKey() {
        byte[] bytes = new byte[bits / 8];
        random.nextBytes(bytes);
        return new SecretKeySpec(bytes, algorithm);
    }
}
