package java.time.temporal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class TemporalQueries {

    private static final TemporalQuery<ZoneId> ZONE_ID = temporal -> temporal.query(
            TemporalQueries.ZONE_ID);
    private static final TemporalQuery<Object> CHRONOLOGY = temporal -> null;
    private static final TemporalQuery<TemporalUnit> PRECISION = temporal -> null;
    private static final TemporalQuery<ZoneOffset> OFFSET = temporal ->
            temporal.isSupported(ChronoField.OFFSET_SECONDS)
                    ? ZoneOffset.ofTotalSeconds(temporal.get(ChronoField.OFFSET_SECONDS)) : null;
    private static final TemporalQuery<ZoneId> ZONE = temporal -> {
        ZoneId zone = temporal.query(ZONE_ID);
        return zone != null ? zone : temporal.query(OFFSET);
    };
    private static final TemporalQuery<LocalDate> LOCAL_DATE = temporal ->
            temporal.isSupported(ChronoField.EPOCH_DAY)
                    ? LocalDate.ofEpochDay(temporal.getLong(ChronoField.EPOCH_DAY)) : null;
    private static final TemporalQuery<LocalTime> LOCAL_TIME = temporal ->
            temporal.isSupported(ChronoField.NANO_OF_DAY)
                    ? LocalTime.ofNanoOfDay(temporal.getLong(ChronoField.NANO_OF_DAY)) : null;

    private TemporalQueries() {
    }

    public static TemporalQuery<ZoneId> zoneId() {
        return ZONE_ID;
    }

    @SuppressWarnings("unchecked")
    public static <T> TemporalQuery<T> chronology() {
        return (TemporalQuery<T>) CHRONOLOGY;
    }

    public static TemporalQuery<TemporalUnit> precision() {
        return PRECISION;
    }

    public static TemporalQuery<ZoneId> zone() {
        return ZONE;
    }

    public static TemporalQuery<ZoneOffset> offset() {
        return OFFSET;
    }

    public static TemporalQuery<LocalDate> localDate() {
        return LOCAL_DATE;
    }

    public static TemporalQuery<LocalTime> localTime() {
        return LOCAL_TIME;
    }
}
