package java.nio.channels;

import java.io.IOException;

public abstract class FileLock implements AutoCloseable {

    private final FileChannel channel;
    private final long position;
    private final long size;
    private final boolean shared;

    protected FileLock(FileChannel channel, long position, long size, boolean shared) {
        this.channel = channel;
        this.position = position;
        this.size = size;
        this.shared = shared;
    }

    public final FileChannel channel() {
        return channel;
    }

    public final long position() {
        return position;
    }

    public final long size() {
        return size;
    }

    public final boolean isShared() {
        return shared;
    }

    public abstract boolean isValid();

    public abstract void release() throws IOException;

    @Override
    public final void close() throws IOException {
        release();
    }
}
