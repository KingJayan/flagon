package dev.flagon.platform;

public interface Clock {

    long currentTimeMillis();

    /** monotonic, arbitrary origin. resolution is not guaranteed, non-decreasing is. */
    long nanoTime();
}
