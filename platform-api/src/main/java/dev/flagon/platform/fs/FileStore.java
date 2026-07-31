package dev.flagon.platform.fs;

/**
 * a hierarchical store of files and directories, addressed by absolute path
 */
public interface FileStore {

    /** @return a name for diagnostics, e.g. the storage this is backed by */
    String name();

    boolean exists(String path);

    boolean isDirectory(String path);

    boolean isFile(String path);

    /** @return the names in a directory, or an empty array when it is not one */
    String[] list(String path);

    /** @return the file's length in bytes, or 0 when it is not a file */
    int size(String path);

    /** @return milliseconds since the epoch, or 0 when unknown */
    long lastModified(String path);

    /**
     * @return the whole file
     * @throws FileStoreException if it does not exist or cannot be read
     */
    byte[] read(String path);

    /** replaces the file's contents, creating it if it does not exist. */
    void write(String path, byte[] data);

    /** @return false if something is already there */
    boolean createFile(String path);

    /** @return false if something is already there, or the parent does not exist */
    boolean createDirectory(String path);

    /** @return false if nothing was there, or a directory was not empty */
    boolean delete(String path);

    /** @return false if the source is missing or the destination is taken */
    boolean move(String from, String to);
}
