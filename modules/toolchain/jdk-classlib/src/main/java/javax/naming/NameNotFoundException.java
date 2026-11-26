package javax.naming;

public class NameNotFoundException extends NamingException {

    public NameNotFoundException() {
    }

    public NameNotFoundException(String explanation) {
        super(explanation);
    }
}
