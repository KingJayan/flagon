package dev.flagon.platform.browser.fs;

import dev.flagon.platform.fs.FileStore;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;
import java.util.Arrays;

final class StoreFileAccessor implements VirtualFileAccessor {

    // how often an open file's writes are queued, so a closed tab loses only the last few seconds
    private static final long QUEUE_MILLIS = 5_000;

    private final WriteBehind writes;
    private final String path;
    private final boolean writable;

    private byte[] data;
    private int length;
    private int position;
    private boolean dirty;
    private boolean closed;
    private long queued = System.currentTimeMillis();

    StoreFileAccessor(FileStore store, WriteBehind writes, String path, boolean writable,
                      boolean append) {
        this.writes = writes;
        this.path = path;
        this.writable = writable;
        byte[] unwritten = writes.get(path);
        this.data = unwritten != null ? unwritten.clone() : store.read(path);
        this.length = data.length;
        this.position = append ? length : 0;
    }

    @Override
    public int read(byte[] into, int offset, int count) throws IOException {
        ensureOpen();
        if (position >= length) {
            return -1;
        }
        int read = Math.min(count, length - position);
        System.arraycopy(data, position, into, offset, read);
        position += read;
        return read;
    }

    @Override
    public void write(byte[] from, int offset, int count) throws IOException {
        ensureOpen();
        if (!writable) {
            throw new IOException("file is not open for writing: " + path);
        }
        ensureCapacity(position + count);
        System.arraycopy(from, offset, data, position, count);
        position += count;
        length = Math.max(length, position);
        dirty = true;
        queueNow();
    }

    private void ensureCapacity(int needed) {
        if (needed <= data.length) {
            return;
        }
        int grown = Math.max(needed, Math.max(32, data.length * 2));
        byte[] larger = new byte[grown];
        System.arraycopy(data, 0, larger, 0, length);
        data = larger;
    }

    @Override
    public int tell() {
        return position;
    }

    @Override
    public void seek(int to) throws IOException {
        ensureOpen();
        if (to < 0) {
            throw new IOException("cannot seek to " + to);
        }
        position = to;
    }

    @Override
    public void skip(int by) throws IOException {
        seek(position + by);
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public void resize(int to) throws IOException {
        ensureOpen();
        if (!writable) {
            throw new IOException("file is not open for writing: " + path);
        }
        ensureCapacity(to);

        for (int at = length; at < to; at++) {
            data[at] = 0;
        }

        length = to;
        position = Math.min(position, length);
        dirty = true;
        queueNow();
    }

    @Override
    public void flush() {
        if (!dirty) {
            return;
        }
        writes.put(path, Arrays.copyOf(data, length));
        queued = System.currentTimeMillis();
        dirty = false;
    }

    private void queueNow() {
        if (System.currentTimeMillis() - queued >= QUEUE_MILLIS) {
            flush();
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        // a write from another thread now fails rather than landing after the last flush
        closed = true;
        flush();
        data = null;
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("file is closed: " + path);
        }
    }
}
