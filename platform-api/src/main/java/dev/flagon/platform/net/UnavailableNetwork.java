package dev.flagon.platform.net;

/**
 * the network of a host that has none
 */
public final class UnavailableNetwork implements Network {

    private final String reason;

    public UnavailableNetwork(String reason) {
        this.reason = reason;
    }

    @Override
    public String name() {
        return "none";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Connection connect(String host, int port) {
        throw new NetworkException("cannot connect to " + host + ":" + port + "; " + reason);
    }
}
