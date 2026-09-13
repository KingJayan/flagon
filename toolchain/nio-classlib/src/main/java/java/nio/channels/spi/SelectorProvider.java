package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public abstract class SelectorProvider {

    public static SelectorProvider provider() {
        throw new UnsupportedOperationException(
                "a page has no selectable channel: the proxy carries a stream, and nothing selects "
                        + "over it. see docs/COMPATIBILITY.md");
    }

    public abstract SocketChannel openSocketChannel() throws IOException;

    public abstract ServerSocketChannel openServerSocketChannel() throws IOException;

    public abstract Selector openSelector() throws IOException;
}
