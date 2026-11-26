package dev.flagon.platform.fs;

import dev.flagon.platform.Resources;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * a file in the store, or one the program shipped beside its jars. a shipped one reads only and
 * is found without asking the store, since the store suspends and a read may hold a lock
 */
final class StoreVirtualFile implements VirtualFile {

    private final FileStore store;
    private final WriteBehind writes;
    private final String path;

    StoreVirtualFile(FileStore store, WriteBehind writes, String path) {
        this.store = store;
        this.writes = writes;
        this.path = path;
    }

    @Override
    public String getName() {
        return FilePaths.name(path);
    }

    private boolean shipped() {
        return Resources.isShippedFile(path);
    }

    private boolean shippedFolder() {
        return Resources.isShippedFolder(path);
    }

    /** makes a shipped folder, and those above it, real, so something can be put in it */
    private boolean stored(String folder) {
        if (store.isDirectory(folder)) {
            return true;
        }
        return !FilePaths.isRoot(folder) && Resources.isShippedFolder(folder)
                && stored(FilePaths.parent(folder)) && store.createDirectory(folder);
    }

    @Override
    public boolean isDirectory() {
        return shippedFolder() || store.isDirectory(path);
    }

    @Override
    public boolean isFile() {
        return shipped() || store.isFile(path);
    }

    @Override
    public String[] listFiles() {
        if (!shippedFolder()) {
            return store.isDirectory(path) ? store.list(path) : null;
        }
        Set<String> names = new LinkedHashSet<>(Arrays.asList(Resources.listShipped(path)));
        if (store.isDirectory(path)) {
            names.addAll(Arrays.asList(store.list(path)));
        }
        return names.toArray(new String[0]);
    }

    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        if (shipped()) {
            return writable ? null : new ShippedFileAccessor(path);
        }
        if (isDirectory()) {
            return null;
        }
        if (!store.exists(path)) {
            if (!writable || !stored(FilePaths.parent(path)) || !store.createFile(path)) {
                return null;
            }
        }
        try {
            return new StoreFileAccessor(store, writes, path, writable, append);
        } catch (FileStoreException e) {
            // another thread moved or deleted it after the check; java.io reports it missing
            return null;
        }
    }

    @Override
    public boolean createFile(String name) throws IOException {
        if (!isDirectory()) {
            throw new IOException("not a directory: " + path);
        }
        String made = FilePaths.join(path, name);
        return !Resources.isShippedFile(made) && stored(path) && store.createFile(made);
    }

    @Override
    public boolean createDirectory(String name) {
        String made = FilePaths.join(path, name);
        return isDirectory() && !Resources.isShippedFolder(made) && stored(path)
                && store.createDirectory(made);
    }

    @Override
    public boolean delete() {
        if (shipped()) {
            return false;
        }
        writes.settle();
        return store.delete(path);
    }

    @Override
    public boolean adopt(VirtualFile file, String name) {
        if (!(file instanceof StoreVirtualFile source) || !isDirectory() || !stored(path)) {
            return false;
        }
        writes.settle();
        return store.move(source.path, FilePaths.join(path, name));
    }

    @Override
    public boolean canRead() {
        return shipped() || shippedFolder() || store.exists(path);
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
        if (shipped()) {
            return Resources.sizeOf(path);
        }
        byte[] unwritten = writes.get(path);
        return unwritten != null ? unwritten.length : store.size(path);
    }
}
