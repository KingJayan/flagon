package java.security.spec;

/** a key as the bytes some standard writes it in */
public abstract class EncodedKeySpec implements KeySpec {

    private final byte[] encoded;

    public EncodedKeySpec(byte[] encoded) {
        this.encoded = encoded.clone();
    }

    public byte[] getEncoded() {
        return encoded.clone();
    }

    public abstract String getFormat();
}
