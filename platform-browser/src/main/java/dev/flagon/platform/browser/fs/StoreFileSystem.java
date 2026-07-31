package dev.flagon.platform.browser.fs;

import dev.flagon.platform.fs.FilePaths;
import dev.flagon.platform.fs.FileStore;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileSystem;
import org.teavm.runtime.fs.VirtualFileSystemProvider;

/**
 * puts a {@link FileStore} behind {@code java.io}
 */
public final class StoreFileSystem implements VirtualFileSystem {

    private final FileStore store;

    public StoreFileSystem(FileStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        this.store = store;
    }

    /** replaces the filesystem every {@code java.io} class in the bundle reads through. */
    public static void install(FileStore store) {
        VirtualFileSystemProvider.setInstance(new StoreFileSystem(store));
    }

    @Override
    public String getUserDir() {
        return FilePaths.ROOT;
    }

    @Override
    public VirtualFile getFile(String path) {
        return new StoreVirtualFile(store, FilePaths.normalize(path));
    }

    @Override
    public String[] getRoots() {
        return new String[] { FilePaths.ROOT };
    }

    @Override
    public boolean isWindows() {
        return false;
    }

    @Override
    public String canonicalize(String path) {
        return FilePaths.normalize(path);
    }
}
