package java.security;

/** thrown when a signature cannot be made or checked */
public class SignatureException extends GeneralSecurityException {

    public SignatureException() {
        super();
    }

    public SignatureException(String message) {
        super(message);
    }

    public SignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
