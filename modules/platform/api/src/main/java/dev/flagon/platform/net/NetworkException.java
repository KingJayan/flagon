package dev.flagon.platform.net;

/** a connection that could not be opened, or one that failed while open */
public class NetworkException extends RuntimeException {

    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
