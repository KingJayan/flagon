package java.time.chrono;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;

public interface ChronoLocalDate extends Temporal, TemporalAdjuster, Comparable<ChronoLocalDate> {

    long toEpochDay();
}
