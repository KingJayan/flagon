package javax.management;

public class JMException extends Exception {

    public JMException() {
    }

    public JMException(String explanation) {
        super(explanation);
    }
}
