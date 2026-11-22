package java.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * a future completed by code. as in the jdk, a stage that fails because the stage before it
 * failed, or because its function threw, holds a CompletionException with the cause inside.
 */
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {

    /** what a stage does once the stage it hangs on settles */
    private interface Settled<T, U> {
        void run(CompletableFuture<U> next, T value, Throwable failure) throws Throwable;
    }

    private static ScheduledThreadPoolExecutor delayer;

    private final List<Runnable> waiting = new ArrayList<>();

    private boolean done;
    private T value;
    private Throwable failure;

    public CompletableFuture() {
    }

    public static <U> CompletableFuture<U> completedFuture(U value) {
        CompletableFuture<U> future = new CompletableFuture<>();
        future.complete(value);
        return future;
    }

    public static <U> CompletableFuture<U> failedFuture(Throwable failure) {
        CompletableFuture<U> future = new CompletableFuture<>();
        future.completeExceptionally(failure);
        return future;
    }

    public static <U> CompletionStage<U> completedStage(U value) {
        return completedFuture(value);
    }

    public static <U> CompletionStage<U> failedStage(Throwable failure) {
        return failedFuture(failure);
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, ASYNC);
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
        return new CompletableFuture<U>().completeAsync(supplier, executor);
    }

    public static CompletableFuture<Void> runAsync(Runnable action) {
        return runAsync(action, ASYNC);
    }

    public static CompletableFuture<Void> runAsync(Runnable action, Executor executor) {
        return supplyAsync(() -> {
            action.run();
            return null;
        }, executor);
    }

    /** completes once every future has, exceptionally if any did */
    public static CompletableFuture<Void> allOf(CompletableFuture<?>... futures) {
        CompletableFuture<Void> all = new CompletableFuture<>();
        if (futures.length == 0) {
            all.complete(null);
            return all;
        }
        int[] left = {futures.length};
        Throwable[] first = {null};
        for (CompletableFuture<?> future : futures) {
            future.whenComplete((ignored, thrown) -> {
                boolean last;
                synchronized (left) {
                    if (thrown != null && first[0] == null) {
                        first[0] = thrown;
                    }
                    last = --left[0] == 0;
                }
                if (last) {
                    if (first[0] != null) {
                        all.settle(null, wrap(first[0]));
                    } else {
                        all.complete(null);
                    }
                }
            });
        }
        return all;
    }

    public static CompletableFuture<Object> anyOf(CompletableFuture<?>... futures) {
        CompletableFuture<Object> any = new CompletableFuture<>();
        for (CompletableFuture<?> future : futures) {
            future.whenComplete((produced, thrown) -> {
                if (thrown != null) {
                    any.settle(null, wrap(thrown));
                } else {
                    any.complete(produced);
                }
            });
        }
        return any;
    }

    public static Executor delayedExecutor(long delay, TimeUnit unit) {
        return delayedExecutor(delay, unit, ASYNC);
    }

    public static Executor delayedExecutor(long delay, TimeUnit unit, Executor executor) {
        if (unit == null || executor == null) {
            throw new NullPointerException();
        }
        return task -> delayer().schedule(() -> executor.execute(task), delay, unit);
    }

    private static synchronized ScheduledThreadPoolExecutor delayer() {
        if (delayer == null) {
            delayer = new ScheduledThreadPoolExecutor(1, task -> {
                Thread thread = new Thread(task, "CompletableFutureDelayScheduler");
                thread.setDaemon(true);
                return thread;
            });
        }
        return delayer;
    }

    /** the jdk's choice: the common pool, or a thread per task when it cannot run two at once */
    private static final Executor ASYNC = task -> {
        if (ForkJoinPool.getCommonPoolParallelism() > 1) {
            ForkJoinPool.commonPool().execute(task);
        } else {
            new Thread(task).start();
        }
    };

    public Executor defaultExecutor() {
        return ASYNC;
    }

    public <U> CompletableFuture<U> newIncompleteFuture() {
        return new CompletableFuture<>();
    }

    public boolean complete(T produced) {
        return settle(produced, null);
    }

    public boolean completeExceptionally(Throwable thrown) {
        if (thrown == null) {
            throw new NullPointerException("failure");
        }
        return settle(null, thrown);
    }

    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
        return completeAsync(supplier, defaultExecutor());
    }

    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
        if (supplier == null || executor == null) {
            throw new NullPointerException();
        }
        executor.execute(() -> {
            try {
                complete(supplier.get());
            } catch (Throwable thrown) {
                settle(null, wrap(thrown));
            }
        });
        return this;
    }

    public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
        if (!isDone()) {
            ScheduledFuture<?> timer = delayer().schedule(
                    () -> completeExceptionally(new TimeoutException()), timeout, unit);
            whenComplete((ignored, thrown) -> timer.cancel(false));
        }
        return this;
    }

    public CompletableFuture<T> completeOnTimeout(T fallback, long timeout, TimeUnit unit) {
        if (!isDone()) {
            ScheduledFuture<?> timer = delayer().schedule(() -> complete(fallback), timeout, unit);
            whenComplete((ignored, thrown) -> timer.cancel(false));
        }
        return this;
    }

    public void obtrudeValue(T produced) {
        synchronized (this) {
            value = produced;
            failure = null;
            done = true;
            notifyAll();
        }
        release();
    }

    public void obtrudeException(Throwable thrown) {
        if (thrown == null) {
            throw new NullPointerException();
        }
        synchronized (this) {
            value = null;
            failure = thrown;
            done = true;
            notifyAll();
        }
        release();
    }

    private boolean settle(T produced, Throwable thrown) {
        synchronized (this) {
            if (done) {
                return false;
            }
            value = produced;
            failure = thrown;
            done = true;
            notifyAll();
        }
        release();
        return true;
    }

    private void release() {
        List<Runnable> stages;
        synchronized (this) {
            stages = new ArrayList<>(waiting);
            waiting.clear();
        }
        for (Runnable stage : stages) {
            stage.run();
        }
    }

    private void onSettled(Runnable stage) {
        if (!done) {
            waiting.add(stage);
            return;
        }
        stage.run();
    }

    /** a failure as a dependent stage holds it */
    static Throwable wrap(Throwable thrown) {
        return thrown instanceof CompletionException ? thrown : new CompletionException(thrown);
    }

    private <U> CompletableFuture<U> then(Executor executor, Settled<T, U> body) {
        CompletableFuture<U> next = newIncompleteFuture();
        onSettled(() -> {
            T produced;
            Throwable thrown;
            synchronized (this) {
                produced = value;
                thrown = failure;
            }
            Runnable step = () -> {
                try {
                    body.run(next, produced, thrown);
                } catch (Throwable failed) {
                    next.settle(null, wrap(failed));
                }
            };
            if (executor == null) {
                step.run();
                return;
            }
            try {
                executor.execute(step);
            } catch (Throwable rejected) {
                next.settle(null, wrap(rejected));
            }
        });
        return next;
    }

    private <U> CompletableFuture<U> apply(Executor executor, Function<? super T, ? extends U> fn) {
        nonNull(fn);
        return then(executor, (next, produced, thrown) -> {
            if (thrown != null) {
                next.settle(null, wrap(thrown));
            } else {
                next.complete(fn.apply(produced));
            }
        });
    }

    private CompletableFuture<Void> accept(Executor executor, Consumer<? super T> action) {
        nonNull(action);
        return apply(executor, produced -> {
            action.accept(produced);
            return null;
        });
    }

    private CompletableFuture<Void> run(Executor executor, Runnable action) {
        nonNull(action);
        return apply(executor, ignored -> {
            action.run();
            return null;
        });
    }

    private <U, V> CompletableFuture<V> both(CompletionStage<? extends U> other, Executor executor,
                                             BiFunction<? super T, ? super U, ? extends V> fn) {
        nonNull(fn);
        CompletableFuture<? extends U> second = other.toCompletableFuture();
        return this.<V>then(null, (next, produced, thrown) -> {
            if (thrown != null) {
                next.settle(null, wrap(thrown));
                return;
            }
            second.whenComplete((theirs, failed) -> {
                if (failed != null) {
                    next.settle(null, wrap(failed));
                    return;
                }
                Runnable step = () -> {
                    try {
                        next.complete(fn.apply(produced, theirs));
                    } catch (Throwable throwing) {
                        next.settle(null, wrap(throwing));
                    }
                };
                if (executor == null) {
                    step.run();
                } else {
                    executor.execute(step);
                }
            });
        });
    }

    private <U> CompletableFuture<U> either(CompletionStage<? extends T> other, Executor executor,
                                            Function<? super T, U> fn) {
        nonNull(fn);
        CompletableFuture<T> first = new CompletableFuture<>();
        BiConsumer<T, Throwable> race = (produced, thrown) -> {
            if (thrown != null) {
                first.settle(null, wrap(thrown));
            } else {
                first.complete(produced);
            }
        };
        whenComplete(race);
        other.toCompletableFuture().whenComplete(race);
        return first.apply(executor, fn);
    }

    private <U> CompletableFuture<U> handle(Executor executor,
                                            BiFunction<? super T, Throwable, ? extends U> fn) {
        nonNull(fn);
        return then(executor,
                (next, produced, thrown) -> next.complete(fn.apply(produced, thrown)));
    }

    private CompletableFuture<T> whenComplete(Executor executor,
                                              BiConsumer<? super T, ? super Throwable> action) {
        nonNull(action);
        return then(executor, (next, produced, thrown) -> {
            try {
                action.accept(produced, thrown);
            } catch (Throwable failed) {
                if (thrown == null) {
                    next.settle(null, wrap(failed));
                    return;
                }
                if (failed != thrown) {
                    thrown.addSuppressed(failed);
                }
            }
            if (thrown != null) {
                next.settle(null, wrap(thrown));
            } else {
                next.complete(produced);
            }
        });
    }

    private <U> CompletableFuture<U> compose(Executor executor,
                                             Function<? super T, ? extends CompletionStage<U>> fn) {
        nonNull(fn);
        return then(executor, (next, produced, thrown) -> {
            if (thrown != null) {
                next.settle(null, wrap(thrown));
            } else {
                relay(fn.apply(produced), next);
            }
        });
    }

    private static <U> void relay(CompletionStage<U> stage, CompletableFuture<U> next) {
        stage.toCompletableFuture().whenComplete((produced, thrown) -> {
            if (thrown != null) {
                next.settle(null, wrap(thrown));
            } else {
                next.complete(produced);
            }
        });
    }

    private CompletableFuture<T> recover(Executor executor, Function<Throwable, ? extends T> fn) {
        nonNull(fn);
        return then(executor, (next, produced, thrown) -> {
            if (thrown == null) {
                next.complete(produced);
            } else {
                next.complete(fn.apply(thrown));
            }
        });
    }

    private CompletableFuture<T> recompose(Executor executor,
                                           Function<Throwable, ? extends CompletionStage<T>> fn) {
        nonNull(fn);
        return then(executor, (next, produced, thrown) -> {
            if (thrown == null) {
                next.complete(produced);
            } else {
                relay(fn.apply(thrown), next);
            }
        });
    }

    private static <V> V nonNull(V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return value;
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return apply(null, fn);
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return apply(defaultExecutor(), fn);
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn,
                                                   Executor executor) {
        return apply(nonNull(executor), fn);
    }

    @Override
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return accept(null, action);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return accept(defaultExecutor(), action);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return accept(nonNull(executor), action);
    }

    @Override
    public CompletableFuture<Void> thenRun(Runnable action) {
        return run(null, action);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return run(defaultExecutor(), action);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return run(nonNull(executor), action);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombine(
            CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return both(other, null, fn);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(
            CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return both(other, defaultExecutor(), fn);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(
            CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn,
            Executor executor) {
        return both(other, nonNull(executor), fn);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other,
                                                      BiConsumer<? super T, ? super U> action) {
        return thenAcceptBothOn(other, null, action);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return thenAcceptBothOn(other, defaultExecutor(), action);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action,
            Executor executor) {
        return thenAcceptBothOn(other, nonNull(executor), action);
    }

    private <U> CompletableFuture<Void> thenAcceptBothOn(CompletionStage<? extends U> other,
                                                         Executor executor,
                                                         BiConsumer<? super T, ? super U> action) {
        nonNull(action);
        return both(other, executor, (mine, theirs) -> {
            action.accept(mine, theirs);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return runAfterBothOn(other, null, action);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return runAfterBothOn(other, defaultExecutor(), action);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action,
                                                     Executor executor) {
        return runAfterBothOn(other, nonNull(executor), action);
    }

    private CompletableFuture<Void> runAfterBothOn(CompletionStage<?> other, Executor executor,
                                                   Runnable action) {
        nonNull(action);
        return both(other, executor, (mine, theirs) -> {
            action.run();
            return null;
        });
    }

    @Override
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other,
                                                  Function<? super T, U> fn) {
        return either(other, null, fn);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other,
                                                       Function<? super T, U> fn) {
        return either(other, defaultExecutor(), fn);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other,
                                                       Function<? super T, U> fn,
                                                       Executor executor) {
        return either(other, nonNull(executor), fn);
    }

    @Override
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other,
                                                Consumer<? super T> action) {
        return acceptEitherOn(other, null, action);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other,
                                                     Consumer<? super T> action) {
        return acceptEitherOn(other, defaultExecutor(), action);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other,
                                                     Consumer<? super T> action,
                                                     Executor executor) {
        return acceptEitherOn(other, nonNull(executor), action);
    }

    private CompletableFuture<Void> acceptEitherOn(CompletionStage<? extends T> other,
                                                   Executor executor,
                                                   Consumer<? super T> action) {
        nonNull(action);
        return either(other, executor, produced -> {
            action.accept(produced);
            return null;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return runAfterEitherOn((CompletionStage<? extends T>) other, null, action);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return runAfterEitherOn((CompletionStage<? extends T>) other, defaultExecutor(), action);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action,
                                                       Executor executor) {
        return runAfterEitherOn((CompletionStage<? extends T>) other, nonNull(executor), action);
    }

    private CompletableFuture<Void> runAfterEitherOn(CompletionStage<? extends T> other,
                                                     Executor executor, Runnable action) {
        nonNull(action);
        return either(other, executor, ignored -> {
            action.run();
            return null;
        });
    }

    @Override
    public <U> CompletableFuture<U> thenCompose(
            Function<? super T, ? extends CompletionStage<U>> fn) {
        return compose(null, fn);
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(
            Function<? super T, ? extends CompletionStage<U>> fn) {
        return compose(defaultExecutor(), fn);
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(
            Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        return compose(nonNull(executor), fn);
    }

    @Override
    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return handle(null, fn);
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return handle(defaultExecutor(), fn);
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn,
                                                Executor executor) {
        return handle(nonNull(executor), fn);
    }

    @Override
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return whenComplete(null, action);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(
            BiConsumer<? super T, ? super Throwable> action) {
        return whenComplete(defaultExecutor(), action);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action,
                                                  Executor executor) {
        return whenComplete(nonNull(executor), action);
    }

    @Override
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return recover(null, fn);
    }

    @Override
    public CompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> fn) {
        return recover(defaultExecutor(), fn);
    }

    @Override
    public CompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> fn,
                                                   Executor executor) {
        return recover(nonNull(executor), fn);
    }

    @Override
    public CompletableFuture<T> exceptionallyCompose(
            Function<Throwable, ? extends CompletionStage<T>> fn) {
        return recompose(null, fn);
    }

    @Override
    public CompletableFuture<T> exceptionallyComposeAsync(
            Function<Throwable, ? extends CompletionStage<T>> fn) {
        return recompose(defaultExecutor(), fn);
    }

    @Override
    public CompletableFuture<T> exceptionallyComposeAsync(
            Function<Throwable, ? extends CompletionStage<T>> fn, Executor executor) {
        return recompose(nonNull(executor), fn);
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    public CompletableFuture<T> copy() {
        return thenApply(Function.identity());
    }

    public CompletionStage<T> minimalCompletionStage() {
        return copy();
    }

    public T join() {
        awaitDone();
        return reported();
    }

    public T getNow(T fallback) {
        if (!done) {
            return fallback;
        }
        return reported();
    }

    public T resultNow() {
        if (!done || failure != null) {
            throw new IllegalStateException(done ? "the task failed" : "the task is not done");
        }
        return value;
    }

    public Throwable exceptionNow() {
        if (!done || failure == null || failure instanceof CancellationException) {
            throw new IllegalStateException("the task did not fail");
        }
        return cause(failure);
    }

    public boolean isCompletedExceptionally() {
        return done && failure != null;
    }

    public int getNumberOfDependents() {
        return waiting.size();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        settle(null, new CancellationException());
        return isCancelled();
    }

    @Override
    public boolean isCancelled() {
        return failure instanceof CancellationException;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        synchronized (this) {
            while (!done) {
                wait();
            }
        }
        return taken();
    }

    @Override
    public T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        synchronized (this) {
            long remaining = unit.toMillis(timeout);
            long deadline = System.currentTimeMillis() + remaining;
            while (!done && remaining > 0L) {
                wait(remaining);
                remaining = deadline - System.currentTimeMillis();
            }
            if (!done) {
                throw new TimeoutException();
            }
        }
        return taken();
    }

    private void awaitDone() {
        boolean interrupted = false;
        synchronized (this) {
            while (!done) {
                try {
                    wait();
                } catch (InterruptedException stopped) {
                    interrupted = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static Throwable cause(Throwable thrown) {
        return thrown instanceof CompletionException && thrown.getCause() != null
                ? thrown.getCause() : thrown;
    }

    private T taken() throws ExecutionException {
        if (failure instanceof CancellationException cancelled) {
            throw cancelled;
        }
        if (failure != null) {
            throw new ExecutionException(cause(failure));
        }
        return value;
    }

    private T reported() {
        if (failure instanceof CancellationException cancelled) {
            throw cancelled;
        }
        if (failure instanceof CompletionException completion) {
            throw completion;
        }
        if (failure != null) {
            throw new CompletionException(failure);
        }
        return value;
    }

    @Override
    public String toString() {
        String state = !done ? (waiting.isEmpty() ? "[Incomplete]"
                : "[Not completed, " + waiting.size() + " dependents]")
                : failure != null ? "[Completed exceptionally: " + failure + "]"
                : "[Completed normally]";
        return super.toString() + state;
    }
}
