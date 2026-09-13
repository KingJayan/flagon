package java.net;

public final class DatagramPacket {

    private byte[] data;
    private int offset;
    private int length;
    private InetAddress address;
    private int port = -1;

    public DatagramPacket(byte[] data, int length) {
        this(data, 0, length);
    }

    public DatagramPacket(byte[] data, int offset, int length) {
        setData(data, offset, length);
    }

    public DatagramPacket(byte[] data, int length, InetAddress address, int port) {
        this(data, 0, length);
        this.address = address;
        this.port = port;
    }

    public DatagramPacket(byte[] data, int offset, int length, InetAddress address, int port) {
        this(data, offset, length);
        this.address = address;
        this.port = port;
    }

    public synchronized byte[] getData() {
        return data;
    }

    public synchronized int getOffset() {
        return offset;
    }

    public synchronized int getLength() {
        return length;
    }

    public synchronized InetAddress getAddress() {
        return address;
    }

    public synchronized int getPort() {
        return port;
    }

    public synchronized void setData(byte[] replacement) {
        setData(replacement, 0, replacement.length);
    }

    public synchronized void setData(byte[] replacement, int from, int count) {
        if (replacement == null) {
            throw new NullPointerException("a packet needs bytes");
        }
        if (from < 0 || count < 0 || from + count > replacement.length) {
            throw new IllegalArgumentException(
                    count + " bytes from " + from + " is outside an array of "
                            + replacement.length);
        }
        this.data = replacement;
        this.offset = from;
        this.length = count;
    }

    public synchronized void setLength(int count) {
        setData(data, offset, count);
    }

    public synchronized void setAddress(InetAddress to) {
        this.address = to;
    }

    public synchronized void setPort(int to) {
        this.port = to;
    }
}
