package dev.flagon.platform.net;

/**
 * a byte stream to one address, in both directions
 */
public interface Connection {

    String host();

    int port();

    /** @return false once the far end has hung up or {@link #close()} has been called */
    boolean isOpen();

    /** @return how many bytes are already here, readable without blocking */
    int available();

    /**
     * reads at least one byte, blocking until there is one.
     *
     * @return the number of bytes read, or -1 at the end of the stream
     * @throws NetworkException if the connection failed
     */
    int read(byte[] into, int offset, int length);

    /** @throws NetworkException if the connection failed */
    void write(byte[] data, int offset, int length);

    /** closes the connection; closing one already closed does nothing. */
    void close();
}
