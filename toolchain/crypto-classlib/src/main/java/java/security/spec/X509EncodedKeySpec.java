package java.security.spec;

/** a public key as X.509 writes one, which is what a server hands out */
public class X509EncodedKeySpec extends EncodedKeySpec {

    public X509EncodedKeySpec(byte[] encoded) {
        super(encoded);
    }

    @Override
    public final String getFormat() {
        return "X.509";
    }
}
