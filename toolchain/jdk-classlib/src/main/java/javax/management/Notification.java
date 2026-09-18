package javax.management;

import java.util.EventObject;

public class Notification extends EventObject {

    private final String type;
    private long sequenceNumber;
    private String message;
    private long timeStamp;
    private Object userData;

    public Notification(String type, Object source, long sequenceNumber) {
        super(source);
        this.type = type;
        this.sequenceNumber = sequenceNumber;
    }

    public Notification(String type, Object source, long sequenceNumber, String message) {
        this(type, source, sequenceNumber);
        this.message = message;
    }

    public Notification(String type, Object source, long sequenceNumber, long timeStamp) {
        this(type, source, sequenceNumber);
        this.timeStamp = timeStamp;
    }

    public Notification(String type, Object source, long sequenceNumber, long timeStamp,
            String message) {
        this(type, source, sequenceNumber, message);
        this.timeStamp = timeStamp;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getType() {
        return type;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public Object getUserData() {
        return userData;
    }

    public void setUserData(Object userData) {
        this.userData = userData;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
