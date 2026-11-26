package java.security;

/** thrown when a key is the wrong kind for what it was handed to */
public class InvalidKeyException extends KeyException {

    public InvalidKeyException() {
        super();
    }

    public InvalidKeyException(String message) {
        super(message);
    }

    public InvalidKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
