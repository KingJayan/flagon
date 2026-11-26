package dev.flagon.platform.wasi;

import dev.flagon.platform.fs.FilePaths;
import dev.flagon.platform.fs.FileStore;
import dev.flagon.platform.fs.FileStoreException;

/** the {@link FileStore} on the directory the host preopened, which {@code /} names */
final class WasiFileStore implements FileStore {

    private final int root;

    private WasiFileStore(int root) {
        this.root = root;
    }

    /** @return the store on the first preopened directory, or null when there is none */
    static WasiFileStore createIfAvailable() {
        int root = Preview1.preopen();
        return root < 0 ? null : new WasiFileStore(root);
    }

    @Override
    public String name() {
        return "wasi";
    }

    @Override
    public boolean exists(String path) {
        return status(path) != null;
    }

    @Override
    public boolean isDirectory(String path) {
        return is(path, Preview1.DIRECTORY);
    }

    @Override
    public boolean isFile(String path) {
        return is(path, Preview1.REGULAR_FILE);
    }

    @Override
    public String[] list(String path) {
        if (!isDirectory(path)) {
            return new String[0];
        }
        String[] names = Preview1.list(root, relative(path));
        return names == null ? new String[0] : names;
    }

    @Override
    public int size(String path) {
        Preview1.Status status = status(path);
        return status == null || status.type() != Preview1.REGULAR_FILE
                ? 0 : (int) Math.min(status.size(), Integer.MAX_VALUE);
    }

    @Override
    public long lastModified(String path) {
        Preview1.Status status = status(path);
        return status == null ? 0L : status.modifiedMillis();
    }

    @Override
    public byte[] read(String path) {
        byte[] data = isFile(path) ? Preview1.read(root, relative(path)) : null;
        if (data == null) {
            throw new FileStoreException("no such file: " + FilePaths.normalize(path));
        }
        return data;
    }

    @Override
    public void write(String path, byte[] data) {
        String at = FilePaths.normalize(path);
        if (isDirectory(at)) {
            throw new FileStoreException("cannot write over a directory: " + at);
        }
        int errno = Preview1.write(root, relative(at), data, false);
        if (errno != 0) {
            throw new FileStoreException("could not write " + at + " (wasi errno " + errno + ")");
        }
    }

    @Override
    public boolean createFile(String path) {
        int errno = Preview1.write(root, relative(path), new byte[0], true);
        if (errno == Preview1.EXISTS) {
            return false;
        }
        if (errno != 0) {
            throw new FileStoreException("could not create " + FilePaths.normalize(path)
                    + " (wasi errno " + errno + ")");
        }
        return true;
    }

    @Override
    public boolean createDirectory(String path) {
        String at = FilePaths.normalize(path);
        return !FilePaths.isRoot(at) && Preview1.createDirectory(root, relative(at)) == 0;
    }

    @Override
    public boolean delete(String path) {
        String at = FilePaths.normalize(path);
        if (isFile(at)) {
            return Preview1.unlink(root, relative(at)) == 0;
        }
        return !FilePaths.isRoot(at) && isDirectory(at)
                && Preview1.removeDirectory(root, relative(at)) == 0;
    }

    @Override
    public boolean move(String from, String to) {
        String source = FilePaths.normalize(from);
        String target = FilePaths.normalize(to);
        return isFile(source) && !exists(target) && isDirectory(FilePaths.parent(target))
                && Preview1.rename(root, relative(source), relative(target)) == 0;
    }

    private boolean is(String path, int type) {
        Preview1.Status status = status(path);
        return status != null && status.type() == type;
    }

    private Preview1.Status status(String path) {
        return Preview1.status(root, relative(path));
    }

    /** wasi resolves below the preopen, so {@code /a/b} is {@code a/b} and the root is {@code .} */
    private static String relative(String path) {
        String at = FilePaths.normalize(path);
        return FilePaths.isRoot(at) ? "." : at.substring(1);
    }
}
