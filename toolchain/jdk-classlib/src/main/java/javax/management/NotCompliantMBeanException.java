package javax.management;

public class NotCompliantMBeanException extends JMException {

    public NotCompliantMBeanException() {
    }

    public NotCompliantMBeanException(String explanation) {
        super(explanation);
    }
}
