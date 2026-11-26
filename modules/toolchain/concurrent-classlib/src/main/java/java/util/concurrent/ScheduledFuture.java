package java.util.concurrent;

/**
 * a result that arrives after a delay the caller named
 */
public interface ScheduledFuture<V> extends Delayed, Future<V> {
}
