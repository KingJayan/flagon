package java.lang.management;

import java.util.Collections;
import java.util.List;

/**
 * what the program was started with
 */
public interface RuntimeMXBean {

    List<String> getInputArguments();

    String getName();

    long getStartTime();

    long getUptime();

    default String getVmName() {
        return "Flagon";
    }

    default List<String> getClassPath() {
        return Collections.emptyList();
    }
}
