package dev.flagon.platform.browser.fs;

import dev.flagon.platform.fs.FilePaths;
import dev.flagon.platform.fs.FileStore;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileSystem;
import org.teavm.runtime.fs.VirtualFileSystemProvider;

import java.util.concurrent.Executor;

public final class StoreFileSystem implements VirtualFileSystem {

    private final FileStore store;
    private final WriteBehind writes;

    /** writes each close and flush to the store before returning */
    public StoreFileSystem(FileStore store) {
        this(store, Runnable::run);
    }

    StoreFileSystem(FileStore store, Executor writer) {
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        this.store = store;
        this.writes = new WriteBehind(store, writer);
    }

    /** closes and flushes reach the store on a thread of their own */
    public static void install(FileStore store) {
        VirtualFileSystemProvider.setInstance(new StoreFileSystem(store,
                task -> new Thread(task, "flagon file writer").start()));
    }

    @Override
    public String getUserDir() {
        return FilePaths.ROOT;
    }

    @Override
    public VirtualFile getFile(String path) {
        return new StoreVirtualFile(store, writes, FilePaths.normalize(path));
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
