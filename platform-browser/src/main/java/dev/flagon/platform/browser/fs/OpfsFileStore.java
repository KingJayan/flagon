package dev.flagon.platform.browser.fs;

import dev.flagon.platform.fs.FilePaths;
import dev.flagon.platform.fs.FileStore;
import dev.flagon.platform.fs.FileStoreException;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * a {@link FileStore} on the origin private file system, which survives a reload.
 *
 * <p>each call suspends the calling green thread until the browser answers; see {@link Opfs}.
 */
public final class OpfsFileStore implements FileStore {

    private OpfsFileStore() {
    }

    /** @return the store, or null when this browser has no origin private file system */
    public static OpfsFileStore createIfAvailable() {
        return Opfs.isAvailable() ? new OpfsFileStore() : null;
    }

    @Override
    public String name() {
        return "opfs";
    }

    @Override
    public boolean exists(String path) {
        return Opfs.stat(FilePaths.normalize(path)).isExists();
    }

    @Override
    public boolean isDirectory(String path) {
        Opfs.Entry entry = Opfs.stat(FilePaths.normalize(path));
        return entry.isExists() && entry.isDirectory();
    }

    @Override
    public boolean isFile(String path) {
        Opfs.Entry entry = Opfs.stat(FilePaths.normalize(path));
        return entry.isExists() && !entry.isDirectory();
    }

    @Override
    public String[] list(String path) {
        if (!isDirectory(path)) {
            return new String[0];
        }
        JSArray<JSString> names = Opfs.list(FilePaths.normalize(path));
        String[] listed = new String[names.getLength()];
        for (int at = 0; at < listed.length; at++) {
            listed[at] = names.get(at).stringValue();
        }
        return listed;
    }

    @Override
    public int size(String path) {
        Opfs.Entry entry = Opfs.stat(FilePaths.normalize(path));
        return entry.isExists() && !entry.isDirectory() ? entry.getSize() : 0;
    }

    @Override
    public long lastModified(String path) {
        return (long) Opfs.stat(FilePaths.normalize(path)).getModified();
    }

    @Override
    public byte[] read(String path) {
        String at = FilePaths.normalize(path);
        if (!isFile(at)) {
            throw new FileStoreException("no such file: " + at);
        }
        Int8Array data = Opfs.read(at);
        byte[] bytes = new byte[data.getLength()];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = data.get(index);
        }
        return bytes;
    }

    @Override
    public void write(String path, byte[] data) {
        String at = FilePaths.normalize(path);
        requireParent(at);
        Opfs.write(at, Int8Array.copyFromJavaArray(data));
    }

    @Override
    public boolean createFile(String path) {
        String at = FilePaths.normalize(path);
        if (exists(at)) {
            return false;
        }
        requireParent(at);
        Opfs.create(at, false);
        return true;
    }

    @Override
    public boolean createDirectory(String path) {
        String at = FilePaths.normalize(path);
        if (exists(at) || !isDirectory(FilePaths.parent(at))) {
            return false;
        }
        Opfs.create(at, true);
        return true;
    }

    @Override
    public boolean delete(String path) {
        String at = FilePaths.normalize(path);
        if (FilePaths.isRoot(at) || !exists(at)) {
            return false;
        }
        // opfs removes a directory and everything under it; java.io does not, so the emptiness
        // check that java.io callers rely on is made here rather than left to the browser
        if (isDirectory(at) && list(at).length > 0) {
            return false;
        }
        Opfs.delete(at);
        return true;
    }

    @Override
    public boolean move(String from, String to) {
        String source = FilePaths.normalize(from);
        String target = FilePaths.normalize(to);
        if (!isFile(source) || exists(target) || !isDirectory(FilePaths.parent(target))) {
            return false;
        }
        Opfs.move(source, target);
        return true;
    }

    private void requireParent(String path) {
        String parent = FilePaths.parent(path);
        if (!isDirectory(parent)) {
            throw new FileStoreException("no such directory: " + parent);
        }
    }
}
