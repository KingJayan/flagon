package java.security.spec;

/** a private key as PKCS#8 writes one */
public class PKCS8EncodedKeySpec extends EncodedKeySpec {

    public PKCS8EncodedKeySpec(byte[] encoded) {
        super(encoded);
    }

    @Override
    public final String getFormat() {
        return "PKCS#8";
    }
}
