package java.lang.management;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;

public final class ManagementFactory {

    private static final long STARTED = System.currentTimeMillis();

    private static final MBeanServer SERVER = new PageMBeanServer();

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

    public static MBeanServer getPlatformMBeanServer() {
        return SERVER;
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

    private static final class PageMBeanServer implements MBeanServer {

        private final Map<ObjectName, Object> registered = new LinkedHashMap<>();

        @Override
        public ObjectInstance registerMBean(Object object, ObjectName name) {
            registered.put(name, object);
            return new ObjectInstance(name, object.getClass().getName());
        }

        @Override
        public void unregisterMBean(ObjectName name) {
            registered.remove(name);
        }

        @Override
        public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
            Set<ObjectName> found = new LinkedHashSet<>();
            for (ObjectName each : registered.keySet()) {
                if ((name == null || name.equals(each)) && (query == null || query.apply(each))) {
                    found.add(each);
                }
            }
            return found;
        }

        @Override
        public boolean isRegistered(ObjectName name) {
            return registered.containsKey(name);
        }
    }
}
