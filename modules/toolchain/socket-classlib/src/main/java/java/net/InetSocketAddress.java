package java.net;

public class InetSocketAddress extends SocketAddress {

    private final String host;
    private final int port;

    public InetSocketAddress(String host, int port) {
        requirePort(port);
        this.host = host == null ? "localhost" : host;
        this.port = port;
    }

    public InetSocketAddress(InetAddress address, int port) {
        this(address == null ? null : address.getHostName(), port);
    }

    public InetSocketAddress(int port) {
        this("localhost", port);
    }

    public static InetSocketAddress createUnresolved(String host, int port) {
        return new InetSocketAddress(host, port);
    }

    public String getHostName() {
        return host;
    }

    public String getHostString() {
        return host;
    }

    public InetAddress getAddress() {
        return InetAddress.of(host);
    }

    public int getPort() {
        return port;
    }

    /** true: nothing here resolves a name, so every address is a name waiting on the proxy */
    public boolean isUnresolved() {
        return true;
    }

    private static void requirePort(int port) {
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port " + port + " is not a port");
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof InetSocketAddress address
                && port == address.port && host.equals(address.host);
    }

    @Override
    public int hashCode() {
        return host.hashCode() * 31 + port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
