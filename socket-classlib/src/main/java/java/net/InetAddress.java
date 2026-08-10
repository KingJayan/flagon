package java.net;

/** a host by name */
public class InetAddress {

    private final String host;

    InetAddress(String host) {
        this.host = host;
    }

    static InetAddress of(String host) {
        return new InetAddress(host);
    }

    public static InetAddress getByName(String host) throws UnknownHostException {
        if (host == null || host.isEmpty()) {
            return new InetAddress("localhost");
        }
        return new InetAddress(host);
    }

    public static InetAddress[] getAllByName(String host) throws UnknownHostException {
        return new InetAddress[] {getByName(host)};
    }

    public static InetAddress getLocalHost() throws UnknownHostException {
        return new InetAddress("localhost");
    }

    public static InetAddress getLoopbackAddress() {
        return new InetAddress("localhost");
    }

    public String getHostName() {
        return host;
    }

    public String getCanonicalHostName() {
        return host;
    }

    /** the name, unresolved; nothing here knows the number behind it */
    public String getHostAddress() {
        return host;
    }

    public byte[] getAddress() {
        return new byte[4];
    }

    public boolean isLoopbackAddress() {
        return "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    /** @return false: without resolving, nothing here can be reached to find out */
    public boolean isReachable(int timeout) {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof InetAddress address && host.equals(address.host);
    }

    @Override
    public int hashCode() {
        return host.hashCode();
    }

    @Override
    public String toString() {
        return host + "/" + host;
    }
}
