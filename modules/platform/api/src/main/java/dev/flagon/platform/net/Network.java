package dev.flagon.platform.net;

import dev.flagon.platform.UnsupportedCapabilityException;

public interface Network {

    String name();

    boolean isAvailable();

    Connection connect(String host, int port);

    default Connection connectSecure(String host, int port) {
        throw new UnsupportedCapabilityException(name() + " carries plain sockets only");
    }
}
