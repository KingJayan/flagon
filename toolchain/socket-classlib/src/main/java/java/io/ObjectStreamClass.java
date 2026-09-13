package java.io;

public class ObjectStreamClass {

    private final String name;

    private ObjectStreamClass(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public long getSerialVersionUID() {
        return 0L;
    }

    public Class<?> forClass() {
        return null;
    }

    public static ObjectStreamClass lookup(Class<?> type) {
        return null;
    }

    public static ObjectStreamClass lookupAny(Class<?> type) {
        return new ObjectStreamClass(type.getName());
    }
}
