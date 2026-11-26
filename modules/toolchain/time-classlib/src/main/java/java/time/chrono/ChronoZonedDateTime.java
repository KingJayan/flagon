package java.time.chrono;

import java.time.Instant;
import java.time.temporal.Temporal;

public interface ChronoZonedDateTime<D extends ChronoLocalDate>
        extends Temporal, Comparable<ChronoZonedDateTime<?>> {

    Instant toInstant();
}
