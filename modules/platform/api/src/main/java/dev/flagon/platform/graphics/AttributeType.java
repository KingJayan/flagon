package dev.flagon.platform.graphics;

/** the component type of a vertex attribute, and how wide one component is. */
public enum AttributeType {
    BYTE(1),
    UNSIGNED_BYTE(1),
    SHORT(2),
    UNSIGNED_SHORT(2),
    INT(4),
    UNSIGNED_INT(4),
    FLOAT(4);

    private final int sizeInBytes;

    AttributeType(int sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public int sizeInBytes() {
        return sizeInBytes;
    }
}
