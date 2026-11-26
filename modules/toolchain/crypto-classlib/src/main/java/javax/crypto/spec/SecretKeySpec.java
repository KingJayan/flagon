package javax.crypto.spec;

import javax.crypto.SecretKey;

/** a secret key as the bytes themselves */
public class SecretKeySpec implements SecretKey, java.security.spec.KeySpec {

    private final byte[] bytes;
    private final String algorithm;

    public SecretKeySpec(byte[] bytes, String algorithm) {
        this(bytes, 0, bytes.length, algorithm);
    }

    public SecretKeySpec(byte[] bytes, int at, int count, String algorithm) {
        if (bytes == null || algorithm == null) {
            throw new IllegalArgumentException("a key needs bytes and an algorithm");
        }
        if (count == 0) {
            throw new IllegalArgumentException("a key of no bytes is not a key");
        }
        this.bytes = new byte[count];
        System.arraycopy(bytes, at, this.bytes, 0, count);
        this.algorithm = algorithm;
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public String getFormat() {
        return "RAW";
    }

    @Override
    public byte[] getEncoded() {
        return bytes.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SecretKeySpec key
                && algorithm.equalsIgnoreCase(key.algorithm)
                && java.util.Arrays.equals(bytes, key.bytes);
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(bytes) * 31
                + algorithm.toLowerCase(java.util.Locale.ROOT).hashCode();
    }
}
