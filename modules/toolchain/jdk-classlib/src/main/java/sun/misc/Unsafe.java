package sun.misc;

import java.lang.reflect.Field;

public final class Unsafe {

    private Unsafe() {
    }

    public static Unsafe getUnsafe() {
        throw refuse();
    }

    public int addressSize() {
        throw refuse();
    }

    public long allocateMemory(long bytes) {
        throw refuse();
    }

    public void freeMemory(long address) {
        throw refuse();
    }

    public void copyMemory(long from, long to, long bytes) {
        throw refuse();
    }

    public void copyMemory(Object from, long fromOffset, Object to, long toOffset, long bytes) {
        throw refuse();
    }

    public int arrayBaseOffset(Class<?> type) {
        throw refuse();
    }

    public int arrayIndexScale(Class<?> type) {
        throw refuse();
    }

    public long objectFieldOffset(Field field) {
        throw refuse();
    }

    public byte getByte(long address) {
        throw refuse();
    }

    public void putByte(long address, byte value) {
        throw refuse();
    }

    public short getShort(long address) {
        throw refuse();
    }

    public void putShort(long address, short value) {
        throw refuse();
    }

    public int getInt(long address) {
        throw refuse();
    }

    public void putInt(long address, int value) {
        throw refuse();
    }

    public long getLong(long address) {
        throw refuse();
    }

    public void putLong(long address, long value) {
        throw refuse();
    }

    public int getInt(Object holder, long offset) {
        throw refuse();
    }

    public void putInt(Object holder, long offset, int value) {
        throw refuse();
    }

    public long getLong(Object holder, long offset) {
        throw refuse();
    }

    public void putLong(Object holder, long offset, long value) {
        throw refuse();
    }

    public Object getObject(Object holder, long offset) {
        throw refuse();
    }

    public void putObject(Object holder, long offset, Object value) {
        throw refuse();
    }

    public int getIntVolatile(Object holder, long offset) {
        throw refuse();
    }

    public void putIntVolatile(Object holder, long offset, int value) {
        throw refuse();
    }

    public long getLongVolatile(Object holder, long offset) {
        throw refuse();
    }

    public void putLongVolatile(Object holder, long offset, long value) {
        throw refuse();
    }

    public Object getObjectVolatile(Object holder, long offset) {
        throw refuse();
    }

    public void putObjectVolatile(Object holder, long offset, Object value) {
        throw refuse();
    }

    public void putOrderedInt(Object holder, long offset, int value) {
        throw refuse();
    }

    public void putOrderedLong(Object holder, long offset, long value) {
        throw refuse();
    }

    public void putOrderedObject(Object holder, long offset, Object value) {
        throw refuse();
    }

    public boolean compareAndSwapInt(Object holder, long offset, int expected, int value) {
        throw refuse();
    }

    public boolean compareAndSwapLong(Object holder, long offset, long expected, long value) {
        throw refuse();
    }

    public boolean compareAndSwapObject(Object holder, long offset, Object expected, Object value) {
        throw refuse();
    }

    public void park(boolean absolute, long time) {
        throw refuse();
    }

    public void unpark(Object thread) {
        throw refuse();
    }

    public void throwException(Throwable cause) {
        throw refuse();
    }

    private static UnsupportedOperationException refuse() {
        return new UnsupportedOperationException(
                "a page has no unsafe memory: nothing here reads or writes an address. see "
                        + "flagon-nu.vercel.app/docs");
    }
}
