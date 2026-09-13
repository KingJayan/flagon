package java.io;

public class ObjectStreamField {

    private final String name;
    private final Class<?> type;

    public ObjectStreamField(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public char getTypeCode() {
        return type.isArray() ? '[' : type.isPrimitive() ? 'I' : 'L';
    }

    public boolean isPrimitive() {
        return type.isPrimitive();
    }
}
