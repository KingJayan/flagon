package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

/** a page has no file handle to seek over, so a stream is the only way through a file */
public abstract class FileChannel implements ByteChannel, GatheringByteChannel,
        ScatteringByteChannel {

    public static class MapMode {

        public static final MapMode READ_ONLY = new MapMode("READ_ONLY");
        public static final MapMode READ_WRITE = new MapMode("READ_WRITE");
        public static final MapMode PRIVATE = new MapMode("PRIVATE");

        private final String name;

        private MapMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public abstract long position() throws IOException;

    public abstract FileChannel position(long to) throws IOException;

    public abstract long size() throws IOException;

    public abstract FileChannel truncate(long size) throws IOException;

    public abstract void force(boolean metadata) throws IOException;

    public abstract long transferTo(long from, long count, WritableByteChannel to)
            throws IOException;

    public abstract long transferFrom(ReadableByteChannel from, long to, long count)
            throws IOException;

    public abstract MappedByteBuffer map(MapMode mode, long from, long size) throws IOException;

    public abstract FileLock lock(long from, long size, boolean shared) throws IOException;

    public abstract FileLock tryLock(long from, long size, boolean shared) throws IOException;

    public final FileLock lock() throws IOException {
        return lock(0L, Long.MAX_VALUE, false);
    }

    public final FileLock tryLock() throws IOException {
        return tryLock(0L, Long.MAX_VALUE, false);
    }

    @Override
    public abstract int read(ByteBuffer buffer) throws IOException;

    @Override
    public abstract int write(ByteBuffer buffer) throws IOException;

    @Override
    public final long read(ByteBuffer[] buffers) throws IOException {
        return read(buffers, 0, buffers.length);
    }

    @Override
    public final long write(ByteBuffer[] buffers) throws IOException {
        return write(buffers, 0, buffers.length);
    }
}
