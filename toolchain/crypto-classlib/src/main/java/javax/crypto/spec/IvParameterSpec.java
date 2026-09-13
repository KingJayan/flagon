package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

/** the block a stream cipher starts from */
public class IvParameterSpec implements AlgorithmParameterSpec {

    private final byte[] iv;

    public IvParameterSpec(byte[] iv) {
        this(iv, 0, iv.length);
    }

    public IvParameterSpec(byte[] iv, int at, int count) {
        if (iv == null) {
            throw new IllegalArgumentException("a starting block needs bytes");
        }
        this.iv = new byte[count];
        System.arraycopy(iv, at, this.iv, 0, count);
    }

    public byte[] getIV() {
        return iv.clone();
    }
}
