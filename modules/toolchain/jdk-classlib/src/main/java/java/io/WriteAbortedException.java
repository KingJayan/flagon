package java.io;

public class WriteAbortedException extends ObjectStreamException {

    public Exception detail;

    public WriteAbortedException(String s, Exception ex) {
        super(s);
        initCause(null);
        detail = ex;
    }

    @Override
    public String getMessage() {
        return detail == null ? super.getMessage() : super.getMessage() + "; " + detail;
    }

    @Override
    public Throwable getCause() {
        return detail;
    }
}
