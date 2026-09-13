package java.nio.channels;

import java.io.IOException;
import java.net.ServerSocket;

public abstract class ServerSocketChannel extends SelectableChannel {

    public static ServerSocketChannel open() throws IOException {
        throw Refusal.refuse();
    }

    @Override
    public final int validOps() {
        return SelectionKey.OP_ACCEPT;
    }

    public abstract ServerSocket socket();

    public abstract SocketChannel accept() throws IOException;
}
