package javax.management;

public class MalformedObjectNameException extends JMException {

    public MalformedObjectNameException() {
    }

    public MalformedObjectNameException(String explanation) {
        super(explanation);
    }
}
