package java.security.spec;

/** a curve for a key pair generator, by name, such as secp256r1 */
public class ECGenParameterSpec extends NamedParameterSpec {

    public ECGenParameterSpec(String stdName) {
        super(stdName);
    }
}
