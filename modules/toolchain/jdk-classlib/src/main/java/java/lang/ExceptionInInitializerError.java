package java.lang;

public class ExceptionInInitializerError extends LinkageError {

    public ExceptionInInitializerError() {
    }

    public ExceptionInInitializerError(String message) {
        super(message);
    }

    public ExceptionInInitializerError(Throwable thrown) {
        initCause(thrown);
    }

    public Throwable getException() {
        return getCause();
    }
}
