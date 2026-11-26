package java.nio.channels.spi;

import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collections;
import java.util.Set;

/** a page has no channel to select, so a select is a wait that only a wakeup or the time ends */
final class EmptySelector extends AbstractSelector {

    private final Object lock = new Object();
    private boolean woken;
    private boolean open = true;

    EmptySelector(SelectorProvider provider) {
        super(provider);
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public Set<SelectionKey> keys() {
        ensureOpen();
        return Collections.emptySet();
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        ensureOpen();
        return Collections.emptySet();
    }

    @Override
    public int selectNow() {
        ensureOpen();
        synchronized (lock) {
            woken = false;
        }
        return 0;
    }

    @Override
    public int select(long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("negative timeout: " + timeout);
        }
        ensureOpen();
        synchronized (lock) {
            long until = System.currentTimeMillis() + timeout;
            while (!woken && open) {
                long left = timeout == 0 ? 0 : until - System.currentTimeMillis();
                if (timeout != 0 && left <= 0) {
                    break;
                }
                try {
                    lock.wait(left);
                } catch (InterruptedException interrupted) {
                    // a select returns early when its thread is interrupted, leaving the flag set
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            woken = false;
        }
        return 0;
    }

    @Override
    public int select() {
        return select(0);
    }

    @Override
    public Selector wakeup() {
        synchronized (lock) {
            woken = true;
            lock.notifyAll();
        }
        return this;
    }

    @Override
    public void close() {
        synchronized (lock) {
            open = false;
            lock.notifyAll();
        }
    }

    private void ensureOpen() {
        if (!open) {
            throw new ClosedSelectorException();
        }
    }
}
