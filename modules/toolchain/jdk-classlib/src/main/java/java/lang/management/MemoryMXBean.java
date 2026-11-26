package java.lang.management;

/**
 * how the heap stands
 */
public interface MemoryMXBean {

    MemoryUsage getHeapMemoryUsage();

    MemoryUsage getNonHeapMemoryUsage();
}
