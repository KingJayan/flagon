package java.util.concurrent.locks;

/**
 * a pair of locks where many can read or one can write, never both at once
 */
public interface ReadWriteLock {

    Lock readLock();

    Lock writeLock();
}
