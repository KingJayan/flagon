package javax.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Locale;

/** hmac over md5, sha-1 and the sha-2 family, from RFC 2104 */
public class Mac implements Cloneable {

    private final String algorithm;
    private final String digestName;
    private final int blockSize;
    private MessageDigest inner;
    private MessageDigest outer;
    private byte[] innerStart;
    private boolean initialized;

    private Mac(String algorithm, String digestName, int blockSize) {
        this.algorithm = algorithm;
        this.digestName = digestName;
        this.blockSize = blockSize;
    }

    public static final Mac getInstance(String algorithm) throws NoSuchAlgorithmException {
        String asked = algorithm.toUpperCase(Locale.ROOT).replace("-", "");
        return switch (asked) {
            case "HMACMD5" -> new Mac(algorithm, "MD5", 64);
            case "HMACSHA1" -> new Mac(algorithm, "SHA-1", 64);
            case "HMACSHA256" -> new Mac(algorithm, "SHA-256", 64);
            case "HMACSHA384" -> new Mac(algorithm, "SHA-384", 128);
            case "HMACSHA512" -> new Mac(algorithm, "SHA-512", 128);
            default -> throw new NoSuchAlgorithmException(algorithm + " is not written here;"
                    + " HmacMD5, HmacSHA1, HmacSHA256, HmacSHA384 and HmacSHA512 are");
        };
    }

    public static final Mac getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException {
        return getInstance(algorithm);
    }

    public final String getAlgorithm() {
        return algorithm;
    }

    public final int getMacLength() {
        try {
            return MessageDigest.getInstance(digestName).getDigestLength();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public final void init(Key key) throws InvalidKeyException {
        byte[] secret = key == null ? null : key.getEncoded();
        if (secret == null) {
            throw new InvalidKeyException("Missing key data");
        }
        try {
            inner = MessageDigest.getInstance(digestName);
            outer = MessageDigest.getInstance(digestName);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        if (secret.length > blockSize) {
            secret = inner.digest(secret);
        }
        byte[] padded = Arrays.copyOf(secret, blockSize);
        innerStart = new byte[blockSize];
        byte[] outerStart = new byte[blockSize];
        for (int at = 0; at < blockSize; at++) {
            innerStart[at] = (byte) (padded[at] ^ 0x36);
            outerStart[at] = (byte) (padded[at] ^ 0x5C);
        }
        outer.update(outerStart);
        inner.update(innerStart);
        initialized = true;
    }

    public final void init(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params != null) {
            throw new InvalidAlgorithmParameterException("HMAC does not use parameters");
        }
        init(key);
    }

    public final void update(byte input) {
        require();
        inner.update(input);
    }

    public final void update(byte[] input) {
        require();
        if (input != null) {
            inner.update(input, 0, input.length);
        }
    }

    public final void update(byte[] input, int offset, int len) {
        require();
        if (input != null) {
            if (offset < 0 || len < 0 || len > input.length - offset) {
                throw new IllegalArgumentException("Bad arguments");
            }
            inner.update(input, offset, len);
        }
    }

    public final void update(ByteBuffer input) {
        require();
        if (input == null) {
            throw new IllegalArgumentException("Buffer must not be null");
        }
        byte[] bytes = new byte[input.remaining()];
        input.get(bytes);
        inner.update(bytes, 0, bytes.length);
    }

    public final byte[] doFinal() {
        require();
        byte[] first = inner.digest();
        MessageDigest closing;
        try {
            closing = (MessageDigest) outer.clone();
        } catch (RuntimeException impossible) {
            throw new IllegalStateException(impossible);
        }
        byte[] made = closing.digest(first);
        inner.update(innerStart);
        return made;
    }

    public final void doFinal(byte[] output, int outOffset) throws ShortBufferException {
        require();
        int length = getMacLength();
        if (output == null || outOffset < 0 || output.length - outOffset < length) {
            throw new ShortBufferException("Cannot store MAC in output buffer");
        }
        System.arraycopy(doFinal(), 0, output, outOffset, length);
    }

    public final byte[] doFinal(byte[] input) {
        update(input);
        return doFinal();
    }

    public final void reset() {
        if (initialized) {
            inner.reset();
            inner.update(innerStart);
        }
    }

    @Override
    public final Object clone() throws CloneNotSupportedException {
        Mac twin = new Mac(algorithm, digestName, blockSize);
        if (initialized) {
            twin.inner = (MessageDigest) inner.clone();
            twin.outer = (MessageDigest) outer.clone();
            twin.innerStart = innerStart;
            twin.initialized = true;
        }
        return twin;
    }

    private void require() {
        if (!initialized) {
            throw new IllegalStateException("MAC not initialized");
        }
    }
}
