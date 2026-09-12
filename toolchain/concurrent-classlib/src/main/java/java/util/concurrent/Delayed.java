package java.util.concurrent;

/**
 * something with a time still to go before it is due
 */
public interface Delayed extends Comparable<Delayed> {

    long getDelay(TimeUnit unit);
}
