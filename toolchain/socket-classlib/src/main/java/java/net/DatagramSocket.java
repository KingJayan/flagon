package java.net;

import java.io.IOException;

public class DatagramSocket implements java.io.Closeable {

    public DatagramSocket() throws SocketException {
        refuse();
    }

    public DatagramSocket(int port) throws SocketException {
        refuse();
    }

    public DatagramSocket(int port, InetAddress address) throws SocketException {
        refuse();
    }

    public DatagramSocket(SocketAddress address) throws SocketException {
        refuse();
    }

    public void send(DatagramPacket packet) throws IOException {
        refuse();
    }

    public void receive(DatagramPacket packet) throws IOException {
        refuse();
    }

    public void bind(SocketAddress address) throws SocketException {
        refuse();
    }

    public void connect(InetAddress address, int port) {
    }

    public void setSoTimeout(int millis) throws SocketException {
    }

    public int getSoTimeout() {
        return 0;
    }

    public void setBroadcast(boolean on) throws SocketException {
    }

    public int getLocalPort() {
        return -1;
    }

    public boolean isClosed() {
        return true;
    }

    @Override
    public void close() {
    }

    static SocketException refuseWith() {
        return new SocketException(
                "a page sends no datagrams: the proxy carries a stream, and nothing carries a "
                        + "packet. see docs/COMPATIBILITY.md");
    }

    private static void refuse() throws SocketException {
        throw refuseWith();
    }
}
