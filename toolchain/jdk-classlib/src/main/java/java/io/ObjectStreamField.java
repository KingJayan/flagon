package java.io;

/**
 * a serializable field, as a class descriptor names it. its signature is interned, as the jdk
 * interns it, since a stream shares one handle between equal type strings.
 */
public class ObjectStreamField implements Comparable<Object> {

    private final String name;
    private final String signature;
    private final Class<?> type;
    private final boolean unshared;
    private int offset;

    public ObjectStreamField(String name, Class<?> type) {
        this(name, type, false);
    }

    public ObjectStreamField(String name, Class<?> type, boolean unshared) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.type = type;
        this.unshared = unshared;
        this.signature = signatureOf(type).intern();
    }

    /** a field read from a stream, whose class may not be here */
    ObjectStreamField(String name, String signature, boolean unshared) {
        this.name = name;
        this.signature = signature.intern();
        this.unshared = unshared;
        this.type = switch (signature.charAt(0)) {
            case 'Z' -> boolean.class;
            case 'B' -> byte.class;
            case 'C' -> char.class;
            case 'S' -> short.class;
            case 'I' -> int.class;
            case 'J' -> long.class;
            case 'F' -> float.class;
            case 'D' -> double.class;
            default -> Object.class;
        };
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public char getTypeCode() {
        return signature.charAt(0);
    }

    public String getTypeString() {
        return isPrimitive() ? null : signature;
    }

    public int getOffset() {
        return offset;
    }

    protected void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean isPrimitive() {
        char code = signature.charAt(0);
        return code != 'L' && code != '[';
    }

    public boolean isUnshared() {
        return unshared;
    }

    String signature() {
        return signature;
    }

    /** primitives first, then by name, as the jdk lays a class's fields out */
    @Override
    public int compareTo(Object other) {
        ObjectStreamField that = (ObjectStreamField) other;
        boolean primitive = isPrimitive();
        if (primitive != that.isPrimitive()) {
            return primitive ? -1 : 1;
        }
        return name.compareTo(that.name);
    }

    @Override
    public String toString() {
        return signature + ' ' + name;
    }

    static String signatureOf(Class<?> type) {
        if (type.isArray()) {
            return "[" + signatureOf(type.getComponentType());
        }
        if (!type.isPrimitive()) {
            return "L" + type.getName().replace('.', '/') + ";";
        }
        return switch (type.getName()) {
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            default -> "V";
        };
    }
}
