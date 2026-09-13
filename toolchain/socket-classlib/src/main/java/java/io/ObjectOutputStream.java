package java.io;

/**
 * java serialization, which a bundle cannot do. see {@link ObjectInputStream} for why.
 */
public class ObjectOutputStream extends OutputStream implements DataOutput {

    public ObjectOutputStream(OutputStream to) throws IOException {
        throw ObjectInputStream.refuse();
    }

    protected ObjectOutputStream() throws IOException {
        throw ObjectInputStream.refuse();
    }

    public final void writeObject(Object what) throws IOException {
        throw ObjectInputStream.refuse();
    }

    public void writeUnshared(Object what) throws IOException {
        throw ObjectInputStream.refuse();
    }

    public void defaultWriteObject() throws IOException {
        throw ObjectInputStream.refuse();
    }

    public void reset() throws IOException {
        throw ObjectInputStream.refuse();
    }

    @Override
    public void write(int value) throws IOException {
        throw ObjectInputStream.refuse();
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        throw ObjectInputStream.refuse();
    }

    @Override
    public void writeByte(int value) throws IOException {
        throw ObjectInputStream.refuse();
    }

    @Override
    public void writeShort(int value) throws IOException {
        throw ObjectInputStream.refuse();
    }

    @Override
    public void writeChar(int value) throws IOException {
        throw ObjectInputStream.refuse();
    }

    @Override
    public void writeInt(int value) throws IOException {
        throw ObjectInputStream.refuse();
    }

    @Override
    public void writeLong(long value) throws IOException {
        throw ObjectInputStream.refuse();
    }

    @Override
    public void writeFloat(float value) throws IOException {
        throw ObjectInputStream.refuse();
    }

    @Override
    public void writeDouble(double value) throws IOException {
        throw ObjectInputStream.refuse();
    }

    @Override
    public void writeBytes(String text) throws IOException {
        throw ObjectInputStream.refuse();
    }

    @Override
    public void writeChars(String text) throws IOException {
        throw ObjectInputStream.refuse();
    }

    @Override
    public void writeUTF(String text) throws IOException {
        throw ObjectInputStream.refuse();
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
