package java.security;

public class UnrecoverableKeyException extends GeneralSecurityException {

    public UnrecoverableKeyException() {
    }

    public UnrecoverableKeyException(String message) {
        super(message);
    }
}
