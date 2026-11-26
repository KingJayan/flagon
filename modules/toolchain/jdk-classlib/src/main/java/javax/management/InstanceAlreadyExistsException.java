package javax.management;

public class InstanceAlreadyExistsException extends JMException {

    public InstanceAlreadyExistsException() {
    }

    public InstanceAlreadyExistsException(String explanation) {
        super(explanation);
    }
}
