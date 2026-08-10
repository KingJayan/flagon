package java.net;

import dev.flagon.runtime.net.NetworkSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * a tcp socket, over whatever the host opens connections with
 */
public class Socket implements java.io.Closeable {

    private final NetworkSocket socket = new NetworkSocket();

    public Socket() {
    }

    public Socket(String host, int port) throws IOException {
        socket.connect(host, port, 0);
    }

    public Socket(InetAddress address, int port) throws IOException {
        socket.connect(address == null ? null : address.getHostName(), port, 0);
    }

    public Socket(String host, int port, InetAddress localAddress, int localPort)
            throws IOException {
        this(host, port);
    }

    public Socket(InetAddress address, int port, InetAddress localAddress, int localPort)
            throws IOException {
        this(address, port);
    }

    public void connect(SocketAddress endpoint) throws IOException {
        connect(endpoint, 0);
    }

    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        if (!(endpoint instanceof InetSocketAddress address)) {
            throw new IllegalArgumentException("a socket connects to an InetSocketAddress");
        }
        socket.connect(address.getHostName(), address.getPort(), timeout);
    }

    public void bind(SocketAddress local) throws IOException {
        throw new SocketException("a page cannot choose the address it goes out from");
    }

    public InputStream getInputStream() throws IOException {
        return socket.inputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return socket.outputStream();
    }

    public InetAddress getInetAddress() {
        return socket.host() == null ? null : InetAddress.of(socket.host());
    }

    public InetAddress getLocalAddress() {
        return InetAddress.of("0.0.0.0");
    }

    public int getPort() {
        return socket.isConnected() ? socket.port() : 0;
    }

    public int getLocalPort() {
        return socket.isConnected() ? 0 : -1;
    }

    public SocketAddress getRemoteSocketAddress() {
        return socket.isConnected()
                ? new InetSocketAddress(socket.host(), socket.port()) : null;
    }

    public SocketAddress getLocalSocketAddress() {
        return null;
    }

    public boolean isConnected() {
        return socket.isConnected();
    }

    public boolean isBound() {
        return socket.isConnected();
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public boolean isInputShutdown() {
        return socket.isInputShutdown();
    }

    public boolean isOutputShutdown() {
        return socket.isOutputShutdown();
    }

    public void shutdownInput() throws IOException {
        socket.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        socket.shutdownOutput();
    }

    public void setSoTimeout(int timeout) throws SocketException {
        try {
            socket.setSoTimeout(timeout);
        } catch (IOException failed) {
            throw new SocketException(failed.getMessage());
        }
    }

    public int getSoTimeout() throws SocketException {
        return socket.getSoTimeout();
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
    }

    public boolean getTcpNoDelay() throws SocketException {
        return true;
    }

    public void setKeepAlive(boolean on) throws SocketException {
    }

    public boolean getKeepAlive() throws SocketException {
        return true;
    }

    public void setTrafficClass(int trafficClass) throws SocketException {
    }

    public int getTrafficClass() throws SocketException {
        return 0;
    }

    public void setReuseAddress(boolean on) throws SocketException {
    }

    public boolean getReuseAddress() throws SocketException {
        return false;
    }

    public void setSendBufferSize(int size) throws SocketException {
    }

    public int getSendBufferSize() throws SocketException {
        return 65_536;
    }

    public void setReceiveBufferSize(int size) throws SocketException {
    }

    public int getReceiveBufferSize() throws SocketException {
        return 65_536;
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
    }

    public int getSoLinger() throws SocketException {
        return -1;
    }

    public void sendUrgentData(int data) throws IOException {
        throw new SocketException("urgent data has no way through the proxy");
    }

    @Override
    public void close() {
        socket.close();
    }

    @Override
    public String toString() {
        return socket.isConnected()
                ? "Socket[addr=" + socket.host() + ",port=" + socket.port() + "]"
                : "Socket[unconnected]";
    }
}
