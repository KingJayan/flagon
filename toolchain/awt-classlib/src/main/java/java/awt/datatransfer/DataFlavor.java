package java.awt.datatransfer;

/**
 * what kind of thing a clipboard is holding
 */
public class DataFlavor {

    public static final DataFlavor stringFlavor = new DataFlavor(String.class, "Unicode String");

    private final Class<?> type;
    private final String name;

    public DataFlavor(Class<?> type, String name) {
        this.type = type;
        this.name = name;
    }

    public Class<?> getRepresentationClass() {
        return type;
    }

    public String getHumanPresentableName() {
        return name;
    }

    public boolean isFlavorTextType() {
        return type == String.class;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DataFlavor flavor && flavor.type == type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return "DataFlavor[" + name + "]";
    }
}
