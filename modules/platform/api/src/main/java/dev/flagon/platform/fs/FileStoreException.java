package dev.flagon.platform.fs;

/** a file operation the store could not carry out. */
public class FileStoreException extends RuntimeException {

    public FileStoreException(String message) {
        super(message);
    }

    public FileStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
