package java.time.chrono;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;

public interface ChronoLocalDateTime<D extends ChronoLocalDate>
        extends Temporal, TemporalAdjuster, Comparable<ChronoLocalDateTime<?>> {
}
