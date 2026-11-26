package java.util.concurrent;

/**
 * a scheduled future that carries the work it is waiting for
 */
public interface RunnableScheduledFuture<V> extends RunnableFuture<V>, ScheduledFuture<V> {

    boolean isPeriodic();
}
