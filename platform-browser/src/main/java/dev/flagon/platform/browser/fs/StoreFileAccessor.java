package dev.flagon.platform.browser.fs;

import dev.flagon.platform.fs.FileStore;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;

final class StoreFileAccessor implements VirtualFileAccessor {

    private final FileStore store;
    private final String path;
    private final boolean writable;
    private final boolean writeThrough;

    private byte[] data;
    private int length;
    private int position;
    private boolean dirty;
    private boolean closed;

    StoreFileAccessor(FileStore store, String path, boolean readable, boolean writable,
                      boolean append) {
        this.store = store;
        this.path = path;
        this.writable = writable;
        this.writeThrough = readable && writable;
        this.data = store.read(path);
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
        if (writeThrough) {
            flush();
        }
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
        if (writeThrough) {
            flush();
        }
    }

    @Override
    public void flush() {
        if (!dirty) {
            return;
        }
        byte[] contents = new byte[length];
        System.arraycopy(data, 0, contents, 0, length);
        store.write(path, contents);
        dirty = false;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        flush();
        closed = true;
        data = null;
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("file is closed: " + path);
        }
    }
}
