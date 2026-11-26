package dev.flagon.platform.browser.fs;

import dev.flagon.platform.fs.FilePaths;
import dev.flagon.platform.fs.FileStore;
import dev.flagon.platform.fs.FileStoreException;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.Int8Array;

public final class OpfsFileStore implements FileStore {

    private final Opfs opfs = new Opfs();

    private OpfsFileStore() {
    }

    public static OpfsFileStore createIfAvailable() {
        if (!Opfs.isAvailable()) {
            return null;
        }
        OpfsFileStore store = new OpfsFileStore();
        return store.opfs.opens() ? store : null;
    }

    @Override
    public String name() {
        return "opfs";
    }

    @Override
    public boolean exists(String path) {
        return opfs.stat(FilePaths.normalize(path)).isExists();
    }

    @Override
    public boolean isDirectory(String path) {
        Opfs.Entry entry = opfs.stat(FilePaths.normalize(path));
        return entry.isExists() && entry.isDirectory();
    }

    @Override
    public boolean isFile(String path) {
        Opfs.Entry entry = opfs.stat(FilePaths.normalize(path));
        return entry.isExists() && !entry.isDirectory();
    }

    @Override
    public String[] list(String path) {
        if (!isDirectory(path)) {
            return new String[0];
        }
        JSArray<JSString> names = opfs.list(FilePaths.normalize(path));
        String[] listed = new String[names.getLength()];
        for (int at = 0; at < listed.length; at++) {
            listed[at] = names.get(at).stringValue();
        }
        return listed;
    }

    @Override
    public int size(String path) {
        Opfs.Entry entry = opfs.stat(FilePaths.normalize(path));
        return entry.isExists() && !entry.isDirectory() ? entry.getSize() : 0;
    }

    @Override
    public long lastModified(String path) {
        return (long) opfs.stat(FilePaths.normalize(path)).getModified();
    }

    @Override
    public byte[] read(String path) {
        String at = FilePaths.normalize(path);
        if (!isFile(at)) {
            throw new FileStoreException("no such file: " + at);
        }
        Int8Array data = opfs.read(at);
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
        opfs.write(at, Int8Array.copyFromJavaArray(data));
    }

    @Override
    public boolean createFile(String path) {
        String at = FilePaths.normalize(path);
        if (exists(at)) {
            return false;
        }
        requireParent(at);
        opfs.create(at, false);
        return true;
    }

    @Override
    public boolean createDirectory(String path) {
        String at = FilePaths.normalize(path);
        if (exists(at) || !isDirectory(FilePaths.parent(at))) {
            return false;
        }
        opfs.create(at, true);
        return true;
    }

    @Override
    public boolean delete(String path) {
        String at = FilePaths.normalize(path);
        if (FilePaths.isRoot(at) || !exists(at)) {
            return false;
        }

        if (isDirectory(at) && list(at).length > 0) {
            return false;
        }
        opfs.delete(at);
        return true;
    }

    @Override
    public boolean move(String from, String to) {
        String source = FilePaths.normalize(from);
        String target = FilePaths.normalize(to);
        if (!isFile(source) || exists(target) || !isDirectory(FilePaths.parent(target))) {
            return false;
        }
        opfs.move(source, target);
        return true;
    }

    private void requireParent(String path) {
        String parent = FilePaths.parent(path);
        if (!isDirectory(parent)) {
            throw new FileStoreException("no such directory: " + parent);
        }
    }
}
