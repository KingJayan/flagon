package java.security;

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.util.Locale;
import java.util.Random;

public abstract class KeyPairGenerator {

    private final String algorithm;

    protected KeyPairGenerator(String algorithm) {
        this.algorithm = algorithm;
    }

    public static KeyPairGenerator getInstance(String algorithm) throws NoSuchAlgorithmException {
        return switch (algorithm.toUpperCase(Locale.ROOT)) {
            case "RSA" -> new RsaPairs(algorithm);
            case "EC" -> new EcPairs(algorithm);
            default -> throw new NoSuchAlgorithmException(
                    algorithm + " key pairs are not made here; rsa and ec are");
        };
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

    public void initialize(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        initialize(params, new SecureRandom());
    }

    public void initialize(AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {
        throw new InvalidAlgorithmParameterException(
                getAlgorithm() + " key pairs take no parameters here");
    }

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

    /** a key pair on a nist curve; secp384r1 unless asked, as in the jdk */
    private static final class EcPairs extends KeyPairGenerator {

        private Ec.Curve curve = Ec.sized(384);
        private Random random = new SecureRandom();

        EcPairs(String algorithm) {
            super(algorithm);
        }

        @Override
        public void initialize(int size) {
            initialize(size, new SecureRandom());
        }

        @Override
        public void initialize(int size, SecureRandom source) {
            Ec.Curve sized = Ec.sized(size);
            if (sized == null) {
                throw new InvalidParameterException("an ec key is 256, 384 or 521 bits here, not "
                        + size);
            }
            this.curve = sized;
            this.random = source;
        }

        @Override
        public void initialize(AlgorithmParameterSpec params, SecureRandom source)
                throws InvalidAlgorithmParameterException {
            Ec.Curve chosen;
            if (params instanceof ECGenParameterSpec named) {
                chosen = Ec.named(named.getName());
            } else if (params instanceof ECParameterSpec described) {
                chosen = Ec.of(described);
            } else {
                throw new InvalidAlgorithmParameterException(
                        "an ec key pair takes an ECGenParameterSpec or ECParameterSpec");
            }
            if (chosen == null) {
                throw new InvalidAlgorithmParameterException("that curve is not written here;"
                        + " secp256r1, secp384r1 and secp521r1 are");
            }
            this.curve = chosen;
            this.random = source;
        }

        @Override
        public KeyPair generateKeyPair() {
            return Ec.generate(curve, random);
        }
    }
}
