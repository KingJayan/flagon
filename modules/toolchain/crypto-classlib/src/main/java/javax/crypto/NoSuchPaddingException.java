package javax.crypto;

import java.security.GeneralSecurityException;

/** thrown when a padding is asked for that is not written here */
public class NoSuchPaddingException extends GeneralSecurityException {

    public NoSuchPaddingException() {
        super();
    }

    public NoSuchPaddingException(String message) {
        super(message);
    }
}
