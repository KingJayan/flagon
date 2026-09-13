package java.nio.channels;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public abstract class SocketChannel extends SelectableChannel
        implements ByteChannel, GatheringByteChannel, ScatteringByteChannel {

    public static SocketChannel open() throws IOException {
        throw Refusal.refuse();
    }

    public static SocketChannel open(SocketAddress address) throws IOException {
        throw Refusal.refuse();
    }

    @Override
    public final int validOps() {
        return SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT;
    }

    public abstract Socket socket();

    public abstract boolean isConnected();

    public abstract boolean isConnectionPending();

    public abstract boolean connect(SocketAddress address) throws IOException;

    public abstract boolean finishConnect() throws IOException;

    @Override
    public abstract int read(ByteBuffer buffer) throws IOException;

    @Override
    public abstract long read(ByteBuffer[] buffers, int offset, int length) throws IOException;

    @Override
    public final long read(ByteBuffer[] buffers) throws IOException {
        return read(buffers, 0, buffers.length);
    }

    @Override
    public abstract int write(ByteBuffer buffer) throws IOException;

    @Override
    public abstract long write(ByteBuffer[] buffers, int offset, int length) throws IOException;

    @Override
    public final long write(ByteBuffer[] buffers) throws IOException {
        return write(buffers, 0, buffers.length);
    }
}
