package dev.flagon.platform.fs;

import dev.flagon.platform.Resources;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;

final class ShippedFileAccessor implements VirtualFileAccessor {

    private final String path;
    private final int size;
    private byte[] data;
    private int position;
    private boolean closed;

    ShippedFileAccessor(String path) {
        this.path = path;
        this.size = Resources.sizeOf(path);
    }

    @Override
    public int read(byte[] into, int offset, int count) throws IOException {
        ensureOpen();
        if (data == null) {
            data = Resources.read(path);
            if (data == null) {
                throw new IOException(path + " has not loaded yet");
            }
        }
        if (position >= size) {
            return -1;
        }
        int read = Math.min(count, size - position);
        System.arraycopy(data, position, into, offset, read);
        position += read;
        return read;
    }

    @Override
    public void write(byte[] from, int offset, int count) throws IOException {
        throw new IOException(path + " ships with the program and is read only");
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
        return size;
    }

    @Override
    public void resize(int to) throws IOException {
        throw new IOException(path + " ships with the program and is read only");
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
        closed = true;
        data = null;
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("file is closed: " + path);
        }
    }
}
