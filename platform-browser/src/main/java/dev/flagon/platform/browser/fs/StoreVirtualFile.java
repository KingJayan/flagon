package dev.flagon.platform.browser.fs;

import dev.flagon.platform.fs.FilePaths;
import dev.flagon.platform.fs.FileStore;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;

final class StoreVirtualFile implements VirtualFile {

    private final FileStore store;
    private final String path;

    StoreVirtualFile(FileStore store, String path) {
        this.store = store;
        this.path = path;
    }

    @Override
    public String getName() {
        return FilePaths.name(path);
    }

    @Override
    public boolean isDirectory() {
        return store.isDirectory(path);
    }

    @Override
    public boolean isFile() {
        return store.isFile(path);
    }

    @Override
    public String[] listFiles() {
        return store.isDirectory(path) ? store.list(path) : null;
    }

    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        if (store.isDirectory(path)) {
            return null;
        }
        if (!store.exists(path)) {
            if (!writable || !store.createFile(path)) {
                return null;
            }
        }
        return new StoreFileAccessor(store, path, readable, writable, append);
    }

    @Override
    public boolean createFile(String name) throws IOException {
        if (!store.isDirectory(path)) {
            throw new IOException("not a directory: " + path);
        }
        return store.createFile(FilePaths.join(path, name));
    }

    @Override
    public boolean createDirectory(String name) {
        return store.isDirectory(path) && store.createDirectory(FilePaths.join(path, name));
    }

    @Override
    public boolean delete() {
        return store.delete(path);
    }

    @Override
    public boolean adopt(VirtualFile file, String name) {
        if (!(file instanceof StoreVirtualFile source) || !store.isDirectory(path)) {
            return false;
        }
        return store.move(source.path, FilePaths.join(path, name));
    }

    @Override
    public boolean canRead() {
        return store.exists(path);
    }

    @Override
    public boolean canWrite() {
        return store.exists(path);
    }

    @Override
    public long lastModified() {
        return store.lastModified(path);
    }

    @Override
    public boolean setLastModified(long lastModified) {
        return false;
    }

    @Override
    public boolean setReadOnly(boolean readOnly) {
        return false;
    }

    @Override
    public int length() {
        return store.size(path);
    }
}
