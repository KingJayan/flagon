package java.util.concurrent;

/**
 * an executor that can also hold work back until a time comes
 */
public interface ScheduledExecutorService extends ExecutorService {

    ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit);

    <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit);

    ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period,
                                           TimeUnit unit);

    ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay,
                                              TimeUnit unit);
}
