package java.security;

/** the two halves of a key, together */
public final class KeyPair {

    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public KeyPair(PublicKey publicKey, PrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public PublicKey getPublic() {
        return publicKey;
    }

    public PrivateKey getPrivate() {
        return privateKey;
    }
}
