package java.util.concurrent;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface CompletionStage<T> {

    <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> mapper);

    <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> mapper);

    CompletionStage<Void> thenAccept(Consumer<? super T> action);

    CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action);

    CompletionStage<Void> thenRun(Runnable action);

    <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> mapper);

    <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
                                          BiFunction<? super T, ? super U, ? extends V> mapper);

    CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action);

    CompletionStage<T> exceptionally(Function<Throwable, ? extends T> recovery);

    CompletableFuture<T> toCompletableFuture();
}
