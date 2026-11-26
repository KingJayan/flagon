package java.util.concurrent;

public abstract class RecursiveAction extends ForkJoinTask<Void> {

    public RecursiveAction() {
    }

    protected abstract void compute();

    @Override
    public final Void getRawResult() {
        return null;
    }

    @Override
    protected final void setRawResult(Void ignored) {
    }

    @Override
    protected final boolean exec() {
        compute();
        return true;
    }
}
