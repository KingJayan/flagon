package javax.crypto;

import java.security.GeneralSecurityException;

/** thrown when there is not enough room for what comes out */
public class ShortBufferException extends GeneralSecurityException {

    public ShortBufferException() {
        super();
    }

    public ShortBufferException(String message) {
        super(message);
    }
}
