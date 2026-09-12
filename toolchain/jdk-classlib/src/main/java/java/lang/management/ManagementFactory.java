package java.lang.management;

import java.util.Collections;
import java.util.List;

public final class ManagementFactory {

    private static final long STARTED = System.currentTimeMillis();

    private ManagementFactory() {
    }

    public static RuntimeMXBean getRuntimeMXBean() {
        return new RuntimeMXBean() {
            @Override
            public List<String> getInputArguments() {
                return Collections.emptyList();
            }

            @Override
            public String getName() {
                return "flagon@browser";
            }

            @Override
            public long getStartTime() {
                return STARTED;
            }

            @Override
            public long getUptime() {
                return System.currentTimeMillis() - STARTED;
            }
        };
    }

    public static MemoryMXBean getMemoryMXBean() {
        return new MemoryMXBean() {
            @Override
            public MemoryUsage getHeapMemoryUsage() {
                Runtime runtime = Runtime.getRuntime();
                long total = runtime.totalMemory();
                return new MemoryUsage(0, total - runtime.freeMemory(), total,
                        runtime.maxMemory());
            }

            @Override
            public MemoryUsage getNonHeapMemoryUsage() {
                return new MemoryUsage(0, 0, 0, 0);
            }
        };
    }
}
