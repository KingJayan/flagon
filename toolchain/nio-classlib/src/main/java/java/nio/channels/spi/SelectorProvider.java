package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public abstract class SelectorProvider {

    private static final SelectorProvider PAGE = new SelectorProvider() {
        @Override
        public SocketChannel openSocketChannel() {
            throw refuse();
        }

        @Override
        public ServerSocketChannel openServerSocketChannel() {
            throw refuse();
        }

        @Override
        public AbstractSelector openSelector() {
            return new EmptySelector(this);
        }
    };

    protected SelectorProvider() {
    }

    /** selectors open, empty, for event loops that also run tasks; channels do not */
    public static SelectorProvider provider() {
        return PAGE;
    }

    public abstract SocketChannel openSocketChannel() throws IOException;

    public abstract ServerSocketChannel openServerSocketChannel() throws IOException;

    public abstract AbstractSelector openSelector() throws IOException;

    private static UnsupportedOperationException refuse() {
        return new UnsupportedOperationException(
                "a page has no selectable channel: the proxy carries a stream, and nothing selects "
                        + "over it. see flagon-nu.vercel.app/docs");
    }
}
