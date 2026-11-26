package java.io;

public class NotActiveException extends ObjectStreamException {

    public NotActiveException() {
    }

    public NotActiveException(String reason) {
        super(reason);
    }
}
