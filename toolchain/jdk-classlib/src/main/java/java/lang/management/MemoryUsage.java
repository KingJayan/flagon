package java.lang.management;

/**
 * how much of the heap is in use, and how much of it there can be
 */
public class MemoryUsage {

    private final long init;
    private final long used;
    private final long committed;
    private final long max;

    public MemoryUsage(long init, long used, long committed, long max) {
        this.init = init;
        this.used = used;
        this.committed = committed;
        this.max = max;
    }

    public long getInit() {
        return init;
    }

    public long getUsed() {
        return used;
    }

    public long getCommitted() {
        return committed;
    }

    public long getMax() {
        return max;
    }
}
