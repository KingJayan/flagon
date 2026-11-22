package java.io;

public class InvalidClassException extends ObjectStreamException {

    public String classname;

    public InvalidClassException(String reason) {
        super(reason);
    }

    public InvalidClassException(String cname, String reason) {
        super(reason);
        classname = cname;
    }

    @Override
    public String getMessage() {
        return classname == null ? super.getMessage() : classname + "; " + super.getMessage();
    }
}
