package javax.crypto;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Locale;

import javax.crypto.spec.IvParameterSpec;

/**
 * encrypts and decrypts
 */
public class Cipher {

    public static final int ENCRYPT_MODE = 1;
    public static final int DECRYPT_MODE = 2;
    public static final int WRAP_MODE = 3;
    public static final int UNWRAP_MODE = 4;

    private static final int RSA = 0;
    private static final int AES_CFB8 = 1;

    private final String transform;
    private final int kind;

    private int direction;
    private java.security.Rsa.Public rsaPublic;
    private java.security.Rsa.Private rsaPrivate;
    private Aes aes;
    private byte[] feedback;

    private Cipher(String transform, int kind) {
        this.transform = transform;
        this.kind = kind;
    }

    public static Cipher getInstance(String transform)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        String asked = transform.toUpperCase(Locale.ROOT);
        if ("RSA".equals(asked) || "RSA/ECB/PKCS1PADDING".equals(asked)
                || "RSA/NONE/PKCS1PADDING".equals(asked)) {
            return new Cipher(transform, RSA);
        }
        if ("AES/CFB8/NOPADDING".equals(asked)) {
            return new Cipher(transform, AES_CFB8);
        }
        throw new NoSuchAlgorithmException(transform + " is not written here; "
                + "RSA/ECB/PKCS1Padding and AES/CFB8/NoPadding are");
    }

    public static Cipher getInstance(String transform, String provider)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        return getInstance(transform);
    }

    public final String getAlgorithm() {
        return transform;
    }

    public final int getBlockSize() {
        return kind == AES_CFB8 ? 1 : 0;
    }

    public final byte[] getIV() {
        return feedback == null ? null : feedback.clone();
    }

    public final int getOutputSize(int inputLength) {
        if (kind == AES_CFB8) {
            return inputLength;
        }
        int size = (rsaPublic != null ? rsaPublic.modulus() : rsaPrivate.modulus()).bitLength();
        return (size + 7) / 8;
    }

    public final void init(int mode, Key key) throws InvalidKeyException {
        try {
            init(mode, key, (AlgorithmParameterSpec) null);
        } catch (InvalidAlgorithmParameterException cannotHappen) {
            throw new InvalidKeyException(cannotHappen.getMessage(), cannotHappen);
        }
    }

    public final void init(int mode, Key key, SecureRandom random) throws InvalidKeyException {
        init(mode, key);
    }

    public final void init(int mode, Key key, AlgorithmParameterSpec parameters)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        this.direction = mode;
        this.rsaPublic = null;
        this.rsaPrivate = null;
        this.aes = null;
        this.feedback = null;

        if (kind == RSA) {
            if (key instanceof java.security.Rsa.Public found) {
                rsaPublic = found;
            } else if (key instanceof java.security.Rsa.Private found) {
                rsaPrivate = found;
            } else {
                throw new InvalidKeyException(
                        "an rsa cipher takes an rsa key, got " + describe(key));
            }
            return;
        }

        byte[] bytes = key.getEncoded();
        if (bytes == null || !"AES".equalsIgnoreCase(key.getAlgorithm())) {
            throw new InvalidKeyException(
                    "an aes cipher takes an aes key, got " + describe(key));
        }
        aes = new Aes(bytes);
        byte[] start = parameters instanceof IvParameterSpec iv ? iv.getIV() : bytes;
        if (start.length < 16) {
            throw new InvalidAlgorithmParameterException(
                    "an aes stream starts from sixteen bytes, got " + start.length);
        }
        feedback = new byte[16];
        System.arraycopy(start, 0, feedback, 0, 16);
    }

    public final void init(int mode, Key key, AlgorithmParameterSpec parameters,
                           SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(mode, key, parameters);
    }

    public final byte[] update(byte[] input) {
        return input == null ? null : update(input, 0, input.length);
    }

    public final byte[] update(byte[] input, int at, int count) {
        requireStream("update");
        byte[] out = new byte[count];
        stream(input, at, count, out, 0);
        return out;
    }

    public final int update(byte[] input, int at, int count, byte[] into)
            throws ShortBufferException {
        return update(input, at, count, into, 0);
    }

    public final int update(byte[] input, int at, int count, byte[] into, int to)
            throws ShortBufferException {
        requireStream("update");
        if (into.length - to < count) {
            throw new ShortBufferException(
                    count + " bytes need " + count + " of room, and there are "
                            + (into.length - to));
        }
        stream(input, at, count, into, to);
        return count;
    }

    public final byte[] doFinal(byte[] input)
            throws IllegalBlockSizeException, BadPaddingException {
        return doFinal(input, 0, input.length);
    }

    public final byte[] doFinal(byte[] input, int at, int count)
            throws IllegalBlockSizeException, BadPaddingException {
        if (kind == AES_CFB8) {
            return update(input, at, count);
        }
        byte[] block = new byte[count];
        System.arraycopy(input, at, block, 0, count);
        return direction == ENCRYPT_MODE || direction == WRAP_MODE
                ? rsaEncrypt(block) : rsaDecrypt(block);
    }

    public final byte[] doFinal() throws IllegalBlockSizeException, BadPaddingException {
        return new byte[0];
    }

    private byte[] rsaEncrypt(byte[] message) throws IllegalBlockSizeException {
        BigInteger modulus = rsaPublic != null ? rsaPublic.modulus() : rsaPrivate.modulus();
        int size = (modulus.bitLength() + 7) / 8;
        if (message.length > size - 11) {
            throw new IllegalBlockSizeException("a message of " + message.length
                    + " bytes does not fit in a " + size + " byte rsa key with its padding");
        }
        byte[] block = new byte[size];
        block[0] = 0;
        block[1] = 2;
        SecureRandom random = new SecureRandom();
        int padTo = size - message.length - 1;
        for (int at = 2; at < padTo; at++) {
            byte[] one = new byte[1];
            do {
                random.nextBytes(one);
            } while (one[0] == 0);
            block[at] = one[0];
        }
        block[padTo] = 0;
        System.arraycopy(message, 0, block, padTo + 1, message.length);

        BigInteger number = new BigInteger(1, block);
        BigInteger turned = rsaPublic != null
                ? java.security.Rsa.apply(number, rsaPublic.exponent(), rsaPublic.modulus())
                : java.security.Rsa.apply(number, rsaPrivate.exponent(), rsaPrivate.modulus());
        return fixedLength(turned, size);
    }

    private byte[] rsaDecrypt(byte[] block) throws BadPaddingException {
        BigInteger modulus = rsaPrivate != null ? rsaPrivate.modulus() : rsaPublic.modulus();
        int size = (modulus.bitLength() + 7) / 8;
        BigInteger number = new BigInteger(1, block);
        BigInteger turned = rsaPrivate != null
                ? java.security.Rsa.apply(number, rsaPrivate.exponent(), rsaPrivate.modulus())
                : java.security.Rsa.apply(number, rsaPublic.exponent(), rsaPublic.modulus());
        byte[] padded = fixedLength(turned, size);

        if (padded[0] != 0 || padded[1] != 2) {
            throw new BadPaddingException(
                    "these bytes do not start the way a padded rsa message does");
        }
        int at = 2;
        while (at < padded.length && padded[at] != 0) {
            at++;
        }
        if (at >= padded.length || at < 10) {
            throw new BadPaddingException(
                    "there is no message after the padding in these rsa bytes");
        }
        byte[] message = new byte[padded.length - at - 1];
        System.arraycopy(padded, at + 1, message, 0, message.length);
        return message;
    }

    private void stream(byte[] input, int at, int count, byte[] into, int to) {
        byte[] keystream = new byte[16];
        for (int step = 0; step < count; step++) {
            aes.encryptBlock(feedback, 0, keystream, 0);
            byte plain = input[at + step];
            byte out = (byte) (plain ^ keystream[0]);
            into[to + step] = out;
            System.arraycopy(feedback, 1, feedback, 0, 15);
            feedback[15] = direction == ENCRYPT_MODE ? out : plain;
        }
    }

    private void requireStream(String call) {
        if (kind != AES_CFB8) {
            throw new IllegalStateException(
                    call + " is for a stream cipher; an rsa cipher works in one doFinal");
        }
        if (aes == null) {
            throw new IllegalStateException("this cipher has no key yet");
        }
    }

    private static byte[] fixedLength(BigInteger value, int size) {
        byte[] bytes = value.toByteArray();
        if (bytes.length == size) {
            return bytes;
        }
        byte[] out = new byte[size];
        if (bytes.length > size) {
            System.arraycopy(bytes, bytes.length - size, out, 0, size);
        } else {
            System.arraycopy(bytes, 0, out, size - bytes.length, bytes.length);
        }
        return out;
    }

    private static String describe(Key key) {
        return key == null ? "nothing" : key.getAlgorithm() + " key";
    }
}
