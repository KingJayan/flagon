package javax.crypto.spec;

import java.security.spec.KeySpec;
import java.util.Arrays;

/** a password, and the salt, rounds and length a key is derived with */
public class PBEKeySpec implements KeySpec {

    private char[] password;
    private final byte[] salt;
    private final int iterationCount;
    private final int keyLength;

    public PBEKeySpec(char[] password) {
        this.password = password == null ? new char[0] : password.clone();
        this.salt = null;
        this.iterationCount = 0;
        this.keyLength = 0;
    }

    public PBEKeySpec(char[] password, byte[] salt, int iterationCount, int keyLength) {
        this.password = password == null ? new char[0] : password.clone();
        if (salt == null) {
            throw new NullPointerException("the salt parameter must be non-null");
        }
        if (salt.length == 0) {
            throw new IllegalArgumentException("the salt parameter must be non-empty");
        }
        if (iterationCount <= 0) {
            throw new IllegalArgumentException("invalid iterationCount value");
        }
        if (keyLength <= 0) {
            throw new IllegalArgumentException("invalid keyLength value");
        }
        this.salt = salt.clone();
        this.iterationCount = iterationCount;
        this.keyLength = keyLength;
    }

    public PBEKeySpec(char[] password, byte[] salt, int iterationCount) {
        this.password = password == null ? new char[0] : password.clone();
        if (salt == null) {
            throw new NullPointerException("the salt parameter must be non-null");
        }
        if (salt.length == 0) {
            throw new IllegalArgumentException("the salt parameter must be non-empty");
        }
        if (iterationCount <= 0) {
            throw new IllegalArgumentException("invalid iterationCount value");
        }
        this.salt = salt.clone();
        this.iterationCount = iterationCount;
        this.keyLength = 0;
    }

    public final void clearPassword() {
        if (password != null) {
            Arrays.fill(password, ' ');
            password = null;
        }
    }

    public final char[] getPassword() {
        if (password == null) {
            throw new IllegalStateException("password has been cleared");
        }
        return password.clone();
    }

    public final byte[] getSalt() {
        return salt == null ? null : salt.clone();
    }

    public final int getIterationCount() {
        return iterationCount;
    }

    public final int getKeyLength() {
        return keyLength;
    }
}
