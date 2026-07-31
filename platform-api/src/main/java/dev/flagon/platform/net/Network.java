package dev.flagon.platform.net;

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
}
