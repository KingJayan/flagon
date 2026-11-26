package java.util.concurrent;

/**
 * a future that carries the work it is waiting for, so whoever runs it settles it
 */
public interface RunnableFuture<V> extends Runnable, Future<V> {

    @Override
    void run();
}
