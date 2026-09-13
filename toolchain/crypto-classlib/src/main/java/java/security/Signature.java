package java.security;

import java.math.BigInteger;
import java.util.Locale;

/**
 * checks that a message came from whoever holds the private key.
 *
 * <p>{@code SHA1withRSA} and {@code SHA256withRSA}, with PKCS#1 type 1 padding. a launcher
 * checks a download with one of these, and the check is the point, so it is done properly
 * rather than waved through.
 */
public abstract class Signature {

    private final String algorithm;

    protected Signature(String algorithm) {
        this.algorithm = algorithm;
    }

    public static Signature getInstance(String algorithm) throws NoSuchAlgorithmException {
        String asked = algorithm.toUpperCase(Locale.ROOT).replace("-", "");
        String digest = switch (asked) {
            case "SHA1WITHRSA", "SHAWITHRSA" -> "SHA-1";
            case "SHA256WITHRSA" -> "SHA-256";
            default -> throw new NoSuchAlgorithmException(algorithm
                    + " is not written here; SHA1withRSA and SHA256withRSA are");
        };
        return new RsaSignature(algorithm, digest);
    }

    public static Signature getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException {
        return getInstance(algorithm);
    }

    public final String getAlgorithm() {
        return algorithm;
    }

    public abstract void initVerify(PublicKey key) throws InvalidKeyException;

    public abstract void initSign(PrivateKey key) throws InvalidKeyException;

    public abstract void update(byte[] bytes, int at, int count) throws SignatureException;

    public final void update(byte[] bytes) throws SignatureException {
        update(bytes, 0, bytes.length);
    }

    public final void update(byte value) throws SignatureException {
        update(new byte[] {value}, 0, 1);
    }

    public abstract boolean verify(byte[] signature) throws SignatureException;

    public abstract byte[] sign() throws SignatureException;

    /** rsa over a digest, padded the way PKCS#1 asks for a signature */
    private static final class RsaSignature extends Signature {

        private final String digestName;

        private MessageDigest digest;
        private Rsa.Public forVerify;
        private Rsa.Private forSign;

        RsaSignature(String algorithm, String digestName) {
            super(algorithm);
            this.digestName = digestName;
        }

        @Override
        public void initVerify(PublicKey key) throws InvalidKeyException {
            if (!(key instanceof Rsa.Public found)) {
                throw new InvalidKeyException("this signature takes an rsa public key");
            }
            forVerify = found;
            forSign = null;
            start();
        }

        @Override
        public void initSign(PrivateKey key) throws InvalidKeyException {
            if (!(key instanceof Rsa.Private found)) {
                throw new InvalidKeyException("this signature takes an rsa private key");
            }
            forSign = found;
            forVerify = null;
            start();
        }

        private void start() throws InvalidKeyException {
            try {
                digest = MessageDigest.getInstance(digestName);
            } catch (NoSuchAlgorithmException missing) {
                throw new InvalidKeyException(digestName + " is not written here", missing);
            }
        }

        @Override
        public void update(byte[] bytes, int at, int count) throws SignatureException {
            if (digest == null) {
                throw new SignatureException("this signature has no key yet");
            }
            digest.update(bytes, at, count);
        }

        @Override
        public boolean verify(byte[] signature) throws SignatureException {
            if (forVerify == null) {
                throw new SignatureException("this signature has no public key to check with");
            }
            int size = (forVerify.modulus().bitLength() + 7) / 8;
            BigInteger number = new BigInteger(1, signature);
            if (number.compareTo(forVerify.modulus()) >= 0) {
                return false;
            }
            byte[] opened = fixedLength(
                    Rsa.apply(number, forVerify.exponent(), forVerify.modulus()), size);
            return MessageDigest.isEqual(opened, padded(digest.digest(), size));
        }

        @Override
        public byte[] sign() throws SignatureException {
            if (forSign == null) {
                throw new SignatureException("this signature has no private key to sign with");
            }
            int size = (forSign.modulus().bitLength() + 7) / 8;
            BigInteger number = new BigInteger(1, padded(digest.digest(), size));
            return fixedLength(Rsa.apply(number, forSign.exponent(), forSign.modulus()), size);
        }

        /**
         * PKCS#1 type 1: a zero, a one, 0xFF until there is room, a zero, then the digest
         * wrapped in the der that names which digest it is.
         */
        private byte[] padded(byte[] made, int size) throws SignatureException {
            byte[] named = Der.join(
                    Der.write(Der.SEQUENCE, Der.join(
                            Der.write(Der.OBJECT_ID, digestId()),
                            Der.write(Der.NULL, new byte[0]))),
                    Der.write(Der.OCTET_STRING, made));
            byte[] wrapped = Der.write(Der.SEQUENCE, named);
            if (wrapped.length > size - 11) {
                throw new SignatureException("this key is too small for a " + digestName
                        + " signature");
            }
            byte[] block = new byte[size];
            block[1] = 1;
            int padTo = size - wrapped.length - 1;
            java.util.Arrays.fill(block, 2, padTo, (byte) 0xFF);
            block[padTo] = 0;
            System.arraycopy(wrapped, 0, block, padTo + 1, wrapped.length);
            return block;
        }

        /** 1.3.14.3.2.26 for sha-1, 2.16.840.1.101.3.4.2.1 for sha-256 */
        private byte[] digestId() {
            return "SHA-1".equals(digestName)
                    ? new byte[] {0x2B, 0x0E, 0x03, 0x02, 0x1A}
                    : new byte[] {0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01};
        }

        private static byte[] fixedLength(BigInteger value, int size) {
            byte[] bytes = value.toByteArray();
            byte[] out = new byte[size];
            if (bytes.length >= size) {
                System.arraycopy(bytes, bytes.length - size, out, 0, size);
            } else {
                System.arraycopy(bytes, 0, out, size - bytes.length, bytes.length);
            }
            return out;
        }
    }
}
