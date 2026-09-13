package java.security;

/** thrown when something is wrong with a key */
public class KeyException extends GeneralSecurityException {

    public KeyException() {
        super();
    }

    public KeyException(String message) {
        super(message);
    }

    public KeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
