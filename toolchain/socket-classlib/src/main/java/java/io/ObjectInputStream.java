package java.io;

/**
 * java serialization
 */
public class ObjectInputStream extends InputStream implements DataInput {

    public ObjectInputStream(InputStream from) throws IOException {
        throw refuse();
    }

    protected ObjectInputStream() throws IOException {
        throw refuse();
    }

    public final Object readObject() throws IOException {
        throw refuse();
    }

    public Object readUnshared() throws IOException {
        throw refuse();
    }

    public void defaultReadObject() throws IOException {
        throw refuse();
    }

    @Override
    public int read() throws IOException {
        throw refuse();
    }

    @Override
    public void readFully(byte[] into) throws IOException {
        throw refuse();
    }

    @Override
    public void readFully(byte[] into, int at, int count) throws IOException {
        throw refuse();
    }

    @Override
    public int skipBytes(int count) throws IOException {
        throw refuse();
    }

    @Override
    public boolean readBoolean() throws IOException {
        throw refuse();
    }

    @Override
    public byte readByte() throws IOException {
        throw refuse();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        throw refuse();
    }

    @Override
    public short readShort() throws IOException {
        throw refuse();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        throw refuse();
    }

    @Override
    public char readChar() throws IOException {
        throw refuse();
    }

    @Override
    public int readInt() throws IOException {
        throw refuse();
    }

    @Override
    public long readLong() throws IOException {
        throw refuse();
    }

    @Override
    public float readFloat() throws IOException {
        throw refuse();
    }

    @Override
    public double readDouble() throws IOException {
        throw refuse();
    }

    @Override
    public String readLine() throws IOException {
        throw refuse();
    }

    @Override
    public String readUTF() throws IOException {
        throw refuse();
    }

    @Override
    public void close() {
    }

    static IOException refuse() {
        return new IOException(
                "java serialisation is not emulated: reading an object back needs a class named "
                        + "at runtime and private fields written from outside, and a bundle has "
                        + "neither. write the fields with DataOutputStream instead. see "
                        + "docs/COMPATIBILITY.md");
    }
}
