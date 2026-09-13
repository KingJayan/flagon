package java.security.spec;

import java.security.GeneralSecurityException;

/** thrown when a key cannot be read out of the form it was written in */
public class InvalidKeySpecException extends GeneralSecurityException {

    public InvalidKeySpecException() {
        super();
    }

    public InvalidKeySpecException(String message) {
        super(message);
    }

    public InvalidKeySpecException(String message, Throwable cause) {
        super(message, cause);
    }
}
