package javax.crypto;

import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Locale;

/** PBKDF2 over hmac, from RFC 8018 */
public class SecretKeyFactory {

    private final String algorithm;
    private final String macName;

    private SecretKeyFactory(String algorithm, String macName) {
        this.algorithm = algorithm;
        this.macName = macName;
    }

    public static final SecretKeyFactory getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        String asked = algorithm.toUpperCase(Locale.ROOT);
        String prefix = "PBKDF2WITH";
        if (asked.startsWith(prefix)) {
            String mac = asked.substring(prefix.length());
            Mac.getInstance(mac);
            return new SecretKeyFactory(algorithm, mac);
        }
        throw new NoSuchAlgorithmException(algorithm + " is not written here;"
                + " PBKDF2WithHmacSHA1, SHA256, SHA384 and SHA512 are");
    }

    public static final SecretKeyFactory getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException {
        return getInstance(algorithm);
    }

    public final String getAlgorithm() {
        return algorithm;
    }

    public final SecretKey generateSecret(KeySpec keySpec) throws InvalidKeySpecException {
        if (!(keySpec instanceof PBEKeySpec spec)) {
            throw new InvalidKeySpecException("Invalid key spec");
        }
        byte[] salt = spec.getSalt();
        if (salt == null) {
            throw new InvalidKeySpecException("Salt not found");
        }
        if (spec.getIterationCount() <= 0) {
            throw new InvalidKeySpecException("Iteration count not found");
        }
        if (spec.getKeyLength() <= 0 || spec.getKeyLength() % 8 != 0) {
            throw new InvalidKeySpecException("Key length not found or not a whole byte count");
        }
        char[] password = spec.getPassword();
        byte[] derived = derive(password, salt, spec.getIterationCount(),
                spec.getKeyLength() / 8);
        return new Derived(algorithm, password, salt, spec.getIterationCount(), derived);
    }

    public final KeySpec getKeySpec(SecretKey key, Class<?> keySpec)
            throws InvalidKeySpecException {
        if (key instanceof PBEKey pbe && keySpec.isAssignableFrom(PBEKeySpec.class)) {
            return new PBEKeySpec(pbe.getPassword(), pbe.getSalt(), pbe.getIterationCount(),
                    pbe.getEncoded().length * 8);
        }
        throw new InvalidKeySpecException("Invalid key spec");
    }

    public final SecretKey translateKey(SecretKey key) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("Missing key");
        }
        if (!algorithm.equalsIgnoreCase(key.getAlgorithm())) {
            throw new InvalidKeyException("The key is not a " + algorithm + " key");
        }
        return key;
    }

    private byte[] derive(char[] password, byte[] salt, int rounds, int length) {
        try {
            Mac mac = Mac.getInstance(macName);
            ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
            byte[] secret = new byte[encoded.remaining()];
            encoded.get(secret);
            // an empty password still keys the hmac, which a key spec cannot hold
            mac.init(new SecretKeySpec(secret.length == 0 ? new byte[1] : secret, macName) {
                @Override
                public byte[] getEncoded() {
                    return secret.clone();
                }
            });
            int size = mac.getMacLength();
            byte[] out = new byte[length];
            for (int block = 1; block * size - size < length; block++) {
                mac.update(salt);
                mac.update(new byte[] {(byte) (block >>> 24), (byte) (block >>> 16),
                    (byte) (block >>> 8), (byte) block});
                byte[] u = mac.doFinal();
                byte[] t = u.clone();
                for (int round = 1; round < rounds; round++) {
                    u = mac.doFinal(u);
                    for (int at = 0; at < t.length; at++) {
                        t[at] ^= u[at];
                    }
                }
                int at = (block - 1) * size;
                System.arraycopy(t, 0, out, at, Math.min(size, length - at));
            }
            return out;
        } catch (NoSuchAlgorithmException | InvalidKeyException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static final class Derived implements PBEKey {

        private final String algorithm;
        private final char[] password;
        private final byte[] salt;
        private final int rounds;
        private final byte[] key;

        Derived(String algorithm, char[] password, byte[] salt, int rounds, byte[] key) {
            this.algorithm = algorithm;
            this.password = password.clone();
            this.salt = salt.clone();
            this.rounds = rounds;
            this.key = key;
        }

        @Override
        public char[] getPassword() {
            return password.clone();
        }

        @Override
        public byte[] getSalt() {
            return salt.clone();
        }

        @Override
        public int getIterationCount() {
            return rounds;
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
            return key.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof SecretKey that
                    && algorithm.equalsIgnoreCase(that.getAlgorithm())
                    && Arrays.equals(key, that.getEncoded());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(key) ^ algorithm.toLowerCase(Locale.ROOT).hashCode();
        }
    }
}
