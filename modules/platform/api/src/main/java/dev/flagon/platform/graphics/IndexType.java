package dev.flagon.platform.graphics;

/** how wide one index in an element buffer is. */
public enum IndexType {
    UNSIGNED_BYTE(1),
    UNSIGNED_SHORT(2),
    UNSIGNED_INT(4);

    private final int sizeInBytes;

    IndexType(int sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public int sizeInBytes() {
        return sizeInBytes;
    }
}
