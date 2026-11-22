package java.util.concurrent;

public abstract class CountedCompleter<T> extends ForkJoinTask<T> {

    final CountedCompleter<?> completer;
    private int pending;

    protected CountedCompleter(CountedCompleter<?> completer, int initialPendingCount) {
        this.completer = completer;
        this.pending = initialPendingCount;
    }

    protected CountedCompleter(CountedCompleter<?> completer) {
        this(completer, 0);
    }

    protected CountedCompleter() {
        this(null, 0);
    }

    public abstract void compute();

    public void onCompletion(CountedCompleter<?> caller) {
    }

    public boolean onExceptionalCompletion(Throwable exception, CountedCompleter<?> caller) {
        return true;
    }

    public final CountedCompleter<?> getCompleter() {
        return completer;
    }

    public final int getPendingCount() {
        return pending;
    }

    public final void setPendingCount(int count) {
        pending = count;
    }

    public final void addToPendingCount(int delta) {
        pending += delta;
    }

    public final boolean compareAndSetPendingCount(int expected, int count) {
        if (pending != expected) {
            return false;
        }
        pending = count;
        return true;
    }

    public final int decrementPendingCountUnlessZero() {
        int was = pending;
        if (was != 0) {
            pending = was - 1;
        }
        return was;
    }

    public final CountedCompleter<?> getRoot() {
        CountedCompleter<?> at = this;
        while (at.completer != null) {
            at = at.completer;
        }
        return at;
    }

    public final void tryComplete() {
        CountedCompleter<?> caller = this;
        CountedCompleter<?> at = this;
        while (true) {
            if (at.decrementPendingCountUnlessZero() != 0) {
                return;
            }
            at.onCompletion(caller);
            at.quietlyComplete();
            if (at.completer == null) {
                return;
            }
            caller = at;
            at = at.completer;
        }
    }

    public final void propagateCompletion() {
        CountedCompleter<?> at = this;
        while (true) {
            if (at.decrementPendingCountUnlessZero() != 0) {
                return;
            }
            at.quietlyComplete();
            if (at.completer == null) {
                return;
            }
            at = at.completer;
        }
    }

    @Override
    public void complete(T value) {
        setRawResult(value);
        onCompletion(this);
        quietlyComplete();
        if (completer != null) {
            completer.tryComplete();
        }
    }

    public final CountedCompleter<?> firstComplete() {
        return decrementPendingCountUnlessZero() == 0 ? this : null;
    }

    public final CountedCompleter<?> nextComplete() {
        quietlyComplete();
        return completer == null ? null : completer.firstComplete();
    }

    public final void quietlyCompleteRoot() {
        getRoot().quietlyComplete();
    }

    public final void helpComplete(int maxTasks) {
    }

    @Override
    void onFailure(Throwable thrown) {
        if (completer != null && onExceptionalCompletion(thrown, this)) {
            completer.finish(thrown);
        }
    }

    @Override
    public T getRawResult() {
        return null;
    }

    @Override
    protected void setRawResult(T value) {
    }

    @Override
    protected final boolean exec() {
        compute();
        return false;
    }
}
