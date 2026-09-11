package dev.flagon.platform.net;

import dev.flagon.platform.UnsupportedCapabilityException;

/**
 * where a host opens outbound connections
 */
public interface Network {

    /** @return a name for diagnostics, e.g. what carries the bytes */
    String name();

    /** @return whether connections can be opened at all here */
    boolean isAvailable();

    /**
     * opens a connection, blocking until it is up.
     *
     * @throws NetworkException if the address refused, is not allowed, or cannot be reached
     */
    Connection connect(String host, int port);

    /**
     * opens a connection with tls under it, and reads and writes plaintext over it.
     *
     * <p>a page cannot speak tls down a socket it does not own, so the host that carries the
     * bytes does the handshake, checks the certificate, and carries plaintext the rest of the
     * way. that plaintext leg is only as private as whatever carries it.
     *
     * @throws UnsupportedCapabilityException if this network carries plain sockets only
     */
    default Connection connectSecure(String host, int port) {
        throw new UnsupportedCapabilityException(name() + " carries plain sockets only");
    }
}
