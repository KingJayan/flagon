package javax.crypto;

import java.security.GeneralSecurityException;

/** thrown when decrypted bytes do not end the way the padding says they should */
public class BadPaddingException extends GeneralSecurityException {

    public BadPaddingException() {
        super();
    }

    public BadPaddingException(String message) {
        super(message);
    }
}
