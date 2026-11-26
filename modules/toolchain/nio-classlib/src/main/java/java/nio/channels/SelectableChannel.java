package java.nio.channels;

import java.io.IOException;

public abstract class SelectableChannel implements Channel {

    public abstract int validOps();

    public boolean isRegistered() {
        return false;
    }

    public SelectionKey keyFor(Selector selector) {
        return null;
    }

    public SelectionKey register(Selector selector, int ops, Object attachment)
            throws ClosedChannelException {
        throw Refusal.refuse();
    }

    public SelectionKey register(Selector selector, int ops) throws ClosedChannelException {
        return register(selector, ops, null);
    }

    public SelectableChannel configureBlocking(boolean blocking) throws IOException {
        throw Refusal.refuse();
    }

    public boolean isBlocking() {
        return true;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void close() throws IOException {
    }
}
