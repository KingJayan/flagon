package java.security;

import java.util.Locale;

public abstract class MessageDigest {

    private final String algorithm;

    protected MessageDigest(String algorithm) {
        this.algorithm = algorithm;
    }

    public static MessageDigest getInstance(String algorithm) throws NoSuchAlgorithmException {
        String asked = algorithm.toUpperCase(Locale.ROOT).replace("-", "");
        Digests.Running running = switch (asked) {
            case "MD5" -> new Digests.Md5();
            case "SHA", "SHA1" -> new Digests.Sha1();
            case "SHA256" -> new Digests.Sha256();
            case "SHA384" -> Digests.Sha512.cut();
            case "SHA512" -> Digests.Sha512.full();
            default -> throw new NoSuchAlgorithmException(algorithm
                    + " is not written here; md5, sha-1, sha-256, sha-384 and sha-512 are");
        };
        return new Written(algorithm, running);
    }

    public static MessageDigest getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException {
        return getInstance(algorithm);
    }

    public final String getAlgorithm() {
        return algorithm;
    }

    public abstract int getDigestLength();

    public abstract void update(byte value);

    public abstract void update(byte[] bytes, int at, int count);

    public final void update(byte[] bytes) {
        update(bytes, 0, bytes.length);
    }

    public abstract byte[] digest();

    public final byte[] digest(byte[] last) {
        update(last);
        return digest();
    }

    public final int digest(byte[] into, int at, int room) throws DigestException {
        byte[] made = digest();
        if (room < made.length) {
            throw new DigestException("a " + getAlgorithm() + " digest is " + made.length
                    + " bytes, and there is room for " + room);
        }
        System.arraycopy(made, 0, into, at, made.length);
        return made.length;
    }

    public abstract void reset();

    @Override
    public abstract Object clone();

    public static boolean isEqual(byte[] left, byte[] right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null || left.length != right.length) {
            return false;
        }
        int different = 0;
        for (int at = 0; at < left.length; at++) {
            different |= left[at] ^ right[at];
        }
        return different == 0;
    }

    @Override
    public String toString() {
        return algorithm + " message digest";
    }

    private static final class Written extends MessageDigest {

        private Digests.Running running;

        Written(String algorithm, Digests.Running running) {
            super(algorithm);
            this.running = running;
        }

        @Override
        public int getDigestLength() {
            return running.length();
        }

        @Override
        public void update(byte value) {
            running.update(new byte[] {value}, 0, 1);
        }

        @Override
        public void update(byte[] bytes, int at, int count) {
            running.update(bytes, at, count);
        }

        @Override
        public byte[] digest() {
            byte[] made = running.finish();
            running.restart();
            return made;
        }

        @Override
        public void reset() {
            running.restart();
        }

        @Override
        public Object clone() {
            return new Written(getAlgorithm(), running.copy());
        }
    }
}
