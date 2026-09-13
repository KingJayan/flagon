package java.security;

import java.util.Locale;
import java.util.Random;

public abstract class KeyPairGenerator {

    private final String algorithm;

    protected KeyPairGenerator(String algorithm) {
        this.algorithm = algorithm;
    }

    public static KeyPairGenerator getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (!"RSA".equals(algorithm.toUpperCase(Locale.ROOT))) {
            throw new NoSuchAlgorithmException(
                    algorithm + " key pairs are not made here; rsa is");
        }
        return new RsaPairs(algorithm);
    }

    public static KeyPairGenerator getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException {
        return getInstance(algorithm);
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public abstract void initialize(int bits);

    public abstract void initialize(int bits, SecureRandom random);

    public abstract KeyPair generateKeyPair();

    public KeyPair genKeyPair() {
        return generateKeyPair();
    }

    private static final class RsaPairs extends KeyPairGenerator {

        private int bits = 1024;
        private Random random = new SecureRandom();

        RsaPairs(String algorithm) {
            super(algorithm);
        }

        @Override
        public void initialize(int size) {
            this.bits = size;
        }

        @Override
        public void initialize(int size, SecureRandom source) {
            this.bits = size;
            this.random = source;
        }

        @Override
        public KeyPair generateKeyPair() {
            return Rsa.generate(bits, random);
        }
    }
}
