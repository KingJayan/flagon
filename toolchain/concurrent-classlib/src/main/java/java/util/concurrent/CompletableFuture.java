package java.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * a value that arrives later, and the chain of work waiting on it
 *
 * <p>the async forms run on the shared pool, which is green threads: the work is ordered rather
 * than parallel, and a stage that never suspends holds the page until it returns.
 */
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {

    private static final ExecutorService POOL = Executors.newFixedThreadPool(4);

    private final List<Runnable> waiting = new ArrayList<>();

    private boolean done;
    private boolean cancelled;
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

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, POOL);
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
        CompletableFuture<U> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable thrown) {
                future.completeExceptionally(thrown);
            }
        });
        return future;
    }

    public static CompletableFuture<Void> runAsync(Runnable action) {
        return runAsync(action, POOL);
    }

    public static CompletableFuture<Void> runAsync(Runnable action, Executor executor) {
        return supplyAsync(() -> {
            action.run();
            return null;
        }, executor);
    }

    public static CompletableFuture<Void> allOf(CompletableFuture<?>... futures) {
        CompletableFuture<Void> all = new CompletableFuture<>();
        if (futures.length == 0) {
            all.complete(null);
            return all;
        }
        int[] left = {futures.length};
        for (CompletableFuture<?> future : futures) {
            future.whenComplete((ignored, thrown) -> {
                if (thrown != null) {
                    all.completeExceptionally(thrown);
                    return;
                }
                boolean last;
                synchronized (left) {
                    last = --left[0] == 0;
                }
                if (last) {
                    all.complete(null);
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
                    any.completeExceptionally(thrown);
                } else {
                    any.complete(produced);
                }
            });
        }
        return any;
    }

    public boolean complete(T produced) {
        return settle(produced, null, false);
    }

    public boolean completeExceptionally(Throwable thrown) {
        if (thrown == null) {
            throw new NullPointerException("failure");
        }
        return settle(null, thrown, false);
    }

    private boolean settle(T produced, Throwable thrown, boolean wasCancelled) {
        List<Runnable> release;
        synchronized (this) {
            if (done) {
                return false;
            }
            value = produced;
            failure = thrown;
            cancelled = wasCancelled;
            done = true;
            release = new ArrayList<>(waiting);
            waiting.clear();
            notifyAll();
        }
        for (Runnable stage : release) {
            stage.run();
        }
        return true;
    }

    /** runs the stage now when the value is already here, and queues it when it is not */
    private void onSettled(Runnable stage) {
        synchronized (this) {
            if (!done) {
                waiting.add(stage);
                return;
            }
        }
        stage.run();
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> mapper) {
        CompletableFuture<U> next = new CompletableFuture<>();
        onSettled(() -> {
            if (failure != null) {
                next.completeExceptionally(failure);
                return;
            }
            try {
                next.complete(mapper.apply(value));
            } catch (Throwable thrown) {
                next.completeExceptionally(thrown);
            }
        });
        return next;
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> mapper) {
        return thenApplyAsync(mapper, POOL);
    }

    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> mapper,
                                                   Executor executor) {
        CompletableFuture<U> next = new CompletableFuture<>();
        onSettled(() -> {
            if (failure != null) {
                next.completeExceptionally(failure);
                return;
            }
            executor.execute(() -> {
                try {
                    next.complete(mapper.apply(value));
                } catch (Throwable thrown) {
                    next.completeExceptionally(thrown);
                }
            });
        });
        return next;
    }

    @Override
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return thenApply(produced -> {
            action.accept(produced);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return thenApplyAsync(produced -> {
            action.accept(produced);
            return null;
        }, POOL);
    }

    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return thenApplyAsync(produced -> {
            action.accept(produced);
            return null;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> thenRun(Runnable action) {
        return thenApply(ignored -> {
            action.run();
            return null;
        });
    }

    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return thenApplyAsync(ignored -> {
            action.run();
            return null;
        }, POOL);
    }

    @Override
    public <U> CompletableFuture<U> thenCompose(
            Function<? super T, ? extends CompletionStage<U>> mapper) {
        CompletableFuture<U> next = new CompletableFuture<>();
        onSettled(() -> {
            if (failure != null) {
                next.completeExceptionally(failure);
                return;
            }
            try {
                mapper.apply(value).toCompletableFuture().whenComplete((produced, thrown) -> {
                    if (thrown != null) {
                        next.completeExceptionally(thrown);
                    } else {
                        next.complete(produced);
                    }
                });
            } catch (Throwable thrown) {
                next.completeExceptionally(thrown);
            }
        });
        return next;
    }

    public <U> CompletableFuture<U> thenComposeAsync(
            Function<? super T, ? extends CompletionStage<U>> mapper) {
        return thenCompose(mapper);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombine(
            CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> mapper) {
        CompletableFuture<V> next = new CompletableFuture<>();
        CompletableFuture<? extends U> second = other.toCompletableFuture();
        onSettled(() -> second.whenComplete((theirs, thrown) -> {
            Throwable failed = failure != null ? failure : thrown;
            if (failed != null) {
                next.completeExceptionally(failed);
                return;
            }
            try {
                next.complete(mapper.apply(value, theirs));
            } catch (Throwable throwing) {
                next.completeExceptionally(throwing);
            }
        }));
        return next;
    }

    @Override
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        CompletableFuture<T> next = new CompletableFuture<>();
        onSettled(() -> {
            try {
                action.accept(value, failure);
            } catch (Throwable ignored) {
                // whenComplete observes the result; it does not get to replace it
            }
            if (failure != null) {
                next.completeExceptionally(failure);
            } else {
                next.complete(value);
            }
        });
        return next;
    }

    @Override
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> recovery) {
        CompletableFuture<T> next = new CompletableFuture<>();
        onSettled(() -> {
            if (failure == null) {
                next.complete(value);
                return;
            }
            try {
                next.complete(recovery.apply(failure));
            } catch (Throwable thrown) {
                next.completeExceptionally(thrown);
            }
        });
        return next;
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    public T join() {
        try {
            return get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new CompletionException(interrupted);
        } catch (ExecutionException failed) {
            throw new CompletionException(failed.getCause());
        }
    }

    public T getNow(T fallback) {
        synchronized (this) {
            if (!done) {
                return fallback;
            }
        }
        return join();
    }

    public boolean isCompletedExceptionally() {
        synchronized (this) {
            return done && failure != null;
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return settle(null, new CancellationException(), true);
    }

    @Override
    public synchronized boolean isCancelled() {
        return cancelled;
    }

    @Override
    public synchronized boolean isDone() {
        return done;
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
        while (!done) {
            wait();
        }
        return taken();
    }

    @Override
    public synchronized T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        long remaining = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + remaining;
        while (!done && remaining > 0L) {
            wait(remaining);
            remaining = deadline - System.currentTimeMillis();
        }
        if (!done) {
            throw new TimeoutException("waited " + unit.toMillis(timeout) + "ms");
        }
        return taken();
    }

    private T taken() throws ExecutionException {
        if (cancelled) {
            throw new CancellationException();
        }
        if (failure != null) {
            throw new ExecutionException(failure);
        }
        return value;
    }
}
