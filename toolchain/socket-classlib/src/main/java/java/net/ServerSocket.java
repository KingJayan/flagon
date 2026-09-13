package java.net;

import java.io.IOException;

/**
 * the socket a page cannot have
 */
public class ServerSocket implements java.io.Closeable {

    public ServerSocket() throws IOException {
        refuse();
    }

    public ServerSocket(int port) throws IOException {
        refuse();
    }

    public ServerSocket(int port, int backlog) throws IOException {
        refuse();
    }

    public ServerSocket(int port, int backlog, InetAddress address) throws IOException {
        refuse();
    }

    public int getLocalPort() {
        return -1;
    }

    public InetAddress getInetAddress() {
        return null;
    }

    public void setReuseAddress(boolean on) {
    }

    public void setReceiveBufferSize(int size) {
    }

    public int getReceiveBufferSize() {
        return 0;
    }

    public SocketAddress getLocalSocketAddress() {
        return null;
    }

    public boolean getReuseAddress() {
        return true;
    }

    public boolean isClosed() {
        return true;
    }

    public boolean isBound() {
        return false;
    }

    public Socket accept() throws IOException {
        return refuse();
    }

    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        bind(endpoint);
    }

    public void bind(SocketAddress endpoint) throws IOException {
        refuse();
    }

    @Override
    public void close() {
    }

    private static Socket refuse() throws IOException {
        throw new SocketException("nothing can connect in to a page, so it cannot listen");
    }
}
