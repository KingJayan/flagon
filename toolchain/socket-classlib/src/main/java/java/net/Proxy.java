package java.net;

/**
 * a proxy an application may name, none of which a page can route through
 *
 * <p>the socket a page opens already goes through one proxy, named by the shell rather than by
 * the application. an application's own choice is carried faithfully so the code that passes it
 * around still runs; it is {@link #NO_PROXY} that decides anything.
 */
public class Proxy {

    public enum Type {
        DIRECT,
        HTTP,
        SOCKS
    }

    public static final Proxy NO_PROXY = new Proxy();

    private final Type type;
    private final SocketAddress address;

    private Proxy() {
        this.type = Type.DIRECT;
        this.address = null;
    }

    public Proxy(Type type, SocketAddress address) {
        if (type == null || type == Type.DIRECT || address == null) {
            throw new IllegalArgumentException("type " + type + " and address " + address
                    + " do not name a proxy");
        }
        this.type = type;
        this.address = address;
    }

    public Type type() {
        return type;
    }

    public SocketAddress address() {
        return address;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Proxy proxy) || type != proxy.type) {
            return false;
        }
        return address == null ? proxy.address == null : address.equals(proxy.address);
    }

    @Override
    public int hashCode() {
        return address == null ? type.hashCode() : type.hashCode() + address.hashCode();
    }

    @Override
    public String toString() {
        return address == null ? "DIRECT" : type + " @ " + address;
    }
}
