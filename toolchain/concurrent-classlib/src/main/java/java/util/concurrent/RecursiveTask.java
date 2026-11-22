package java.util.concurrent;

public abstract class RecursiveTask<V> extends ForkJoinTask<V> {

    private V result;

    public RecursiveTask() {
    }

    protected abstract V compute();

    @Override
    public final V getRawResult() {
        return result;
    }

    @Override
    protected final void setRawResult(V value) {
        result = value;
    }

    @Override
    protected final boolean exec() {
        result = compute();
        return true;
    }
}
