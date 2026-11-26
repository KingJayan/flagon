package java.security;

/** thrown when an algorithm is handed a setting it cannot use */
public class InvalidAlgorithmParameterException extends GeneralSecurityException {

    public InvalidAlgorithmParameterException() {
        super();
    }

    public InvalidAlgorithmParameterException(String message) {
        super(message);
    }
}
