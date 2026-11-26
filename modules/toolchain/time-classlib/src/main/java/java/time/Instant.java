package java.time;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;

public final class Instant implements Temporal, TemporalAdjuster, Comparable<Instant> {

    public static final Instant EPOCH = new Instant(0, 0);
    public static final Instant MIN = new Instant(-31557014167219200L, 0);
    public static final Instant MAX = new Instant(31556889864403199L, 999_999_999);

    private final long seconds;
    private final int nanos;

    private Instant(long epochSecond, int nanos) {
        this.seconds = epochSecond;
        this.nanos = nanos;
    }

    private static Instant create(long seconds, int nanoOfSecond) {
        if ((seconds | nanoOfSecond) == 0) {
            return EPOCH;
        }
        if (seconds < MIN.seconds || seconds > MAX.seconds) {
            throw new DateTimeException("Instant exceeds minimum or maximum instant");
        }
        return new Instant(seconds, nanoOfSecond);
    }

    public static Instant now() {
        return Clock.systemUTC().instant();
    }

    public static Instant now(Clock clock) {
        return clock.instant();
    }

    public static Instant ofEpochSecond(long epochSecond) {
        return create(epochSecond, 0);
    }

    public static Instant ofEpochSecond(long epochSecond, long nanoAdjustment) {
        long secs = Math.addExact(epochSecond, Math.floorDiv(nanoAdjustment, 1_000_000_000L));
        return create(secs, (int) Math.floorMod(nanoAdjustment, 1_000_000_000L));
    }

    public static Instant ofEpochMilli(long epochMilli) {
        return create(Math.floorDiv(epochMilli, 1000L), (int) Math.floorMod(epochMilli,
                1000L) * 1_000_000);
    }

    public static Instant from(TemporalAccessor temporal) {
        if (temporal instanceof Instant instant) {
            return instant;
        }
        try {
            long instantSecs = temporal.getLong(ChronoField.INSTANT_SECONDS);
            int nanoOfSecond = temporal.get(ChronoField.NANO_OF_SECOND);
            return Instant.ofEpochSecond(instantSecs, nanoOfSecond);
        } catch (DateTimeException e) {
            throw new DateTimeException("Unable to obtain Instant from TemporalAccessor: "
                    + temporal + " of type " + temporal.getClass().getName(), e);
        }
    }

    public static Instant parse(CharSequence text) {
        return DateTimeFormatter.ISO_INSTANT.parse(text, Instant::from);
    }

    /** @return the whole units in a duration, truncated toward zero */
    static long amount(Duration duration, TemporalUnit unit) {
        if (duration.isNegative()) {
            return -amount(duration.negated(), unit);
        }
        long unitNanos = unit.getDuration().toNanos();
        if (unitNanos >= 1_000_000_000L) {
            return duration.getSeconds() / (unitNanos / 1_000_000_000L);
        }
        return Math.addExact(Math.multiplyExact(duration.getSeconds(), 1_000_000_000L / unitNanos),
                duration.getNano() / unitNanos);
    }

    @Override
    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return field == ChronoField.INSTANT_SECONDS || field == ChronoField.NANO_OF_SECOND
                    || field == ChronoField.MICRO_OF_SECOND || field == ChronoField.MILLI_OF_SECOND;
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isTimeBased() || unit == ChronoUnit.DAYS;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public int get(TemporalField field) {
        if (field == ChronoField.INSTANT_SECONDS) {
            throw new UnsupportedTemporalTypeException(
                    "Invalid field 'InstantSeconds' for get() method, use getLong() instead");
        }
        return (int) getLong(field);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field instanceof ChronoField chrono) {
            switch (chrono) {
                case NANO_OF_SECOND:
                    return nanos;
                case MICRO_OF_SECOND:
                    return nanos / 1000;
                case MILLI_OF_SECOND:
                    return nanos / 1_000_000;
                case INSTANT_SECONDS:
                    return seconds;
                default:
                    throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            }
        }
        return field.getFrom(this);
    }

    public long getEpochSecond() {
        return seconds;
    }

    public int getNano() {
        return nanos;
    }

    @Override
    public Instant with(TemporalAdjuster adjuster) {
        return (Instant) adjuster.adjustInto(this);
    }

    @Override
    public Instant with(TemporalField field, long newValue) {
        if (field instanceof ChronoField chrono) {
            chrono.checkValidValue(newValue);
            switch (chrono) {
                case MILLI_OF_SECOND:
                    return create(seconds, (int) newValue * 1_000_000);
                case MICRO_OF_SECOND:
                    return create(seconds, (int) newValue * 1000);
                case NANO_OF_SECOND:
                    return create(seconds, (int) newValue);
                case INSTANT_SECONDS:
                    return create(newValue, nanos);
                default:
                    throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            }
        }
        return field.adjustInto(this, newValue);
    }

    public Instant truncatedTo(TemporalUnit unit) {
        if (unit == ChronoUnit.NANOS) {
            return this;
        }
        long dur = unit.getDuration().toNanos();
        if (unit.getDuration().getSeconds() > 86400 || (86_400_000_000_000L % dur) != 0) {
            throw new UnsupportedTemporalTypeException(
                    "Unit must divide into a standard day without remainder");
        }
        long nod = Math.floorMod(seconds, 86400L) * 1_000_000_000L + nanos;
        long result = Math.floorDiv(nod, dur) * dur;
        return plusNanos(result - nod);
    }

    @Override
    public Instant plus(TemporalAmount amountToAdd) {
        return (Instant) amountToAdd.addTo(this);
    }

    @Override
    public Instant plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit chrono) {
            switch (chrono) {
                case NANOS:
                    return plusNanos(amountToAdd);
                case MICROS:
                    return plus(amountToAdd / 1_000_000, (amountToAdd % 1_000_000) * 1000);
                case MILLIS:
                    return plusMillis(amountToAdd);
                case SECONDS:
                    return plusSeconds(amountToAdd);
                case MINUTES:
                    return plusSeconds(Math.multiplyExact(amountToAdd, 60L));
                case HOURS:
                    return plusSeconds(Math.multiplyExact(amountToAdd, 3600L));
                case HALF_DAYS:
                    return plusSeconds(Math.multiplyExact(amountToAdd, 43200L));
                case DAYS:
                    return plusSeconds(Math.multiplyExact(amountToAdd, 86400L));
                default:
                    throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
            }
        }
        return unit.addTo(this, amountToAdd);
    }

    public Instant plusSeconds(long secondsToAdd) {
        return plus(secondsToAdd, 0);
    }

    public Instant plusMillis(long millisToAdd) {
        return plus(millisToAdd / 1000, (millisToAdd % 1000) * 1_000_000);
    }

    public Instant plusNanos(long nanosToAdd) {
        return plus(0, nanosToAdd);
    }

    private Instant plus(long secondsToAdd, long nanosToAdd) {
        if ((secondsToAdd | nanosToAdd) == 0) {
            return this;
        }
        long epochSec = Math.addExact(seconds, secondsToAdd);
        epochSec = Math.addExact(epochSec, nanosToAdd / 1_000_000_000L);
        nanosToAdd = nanosToAdd % 1_000_000_000L;
        return ofEpochSecond(epochSec, nanos + nanosToAdd);
    }

    @Override
    public Instant minus(TemporalAmount amountToSubtract) {
        return (Instant) amountToSubtract.subtractFrom(this);
    }

    @Override
    public Instant minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE
                ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit);
    }

    public Instant minusSeconds(long secondsToSubtract) {
        return secondsToSubtract == Long.MIN_VALUE
                ? plusSeconds(Long.MAX_VALUE).plusSeconds(1) : plusSeconds(-secondsToSubtract);
    }

    public Instant minusMillis(long millisToSubtract) {
        return millisToSubtract == Long.MIN_VALUE
                ? plusMillis(Long.MAX_VALUE).plusMillis(1) : plusMillis(-millisToSubtract);
    }

    public Instant minusNanos(long nanosToSubtract) {
        return nanosToSubtract == Long.MIN_VALUE
                ? plusNanos(Long.MAX_VALUE).plusNanos(1) : plusNanos(-nanosToSubtract);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.precision()) {
            return (R) ChronoUnit.NANOS;
        }
        if (query == TemporalQueries.localDate() || query == TemporalQueries.localTime()
                || query == TemporalQueries.offset() || query == TemporalQueries.zone()) {
            return null;
        }
        return Temporal.super.query(query);
    }

    @Override
    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.INSTANT_SECONDS, seconds)
                .with(ChronoField.NANO_OF_SECOND, nanos);
    }

    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        Instant end = Instant.from(endExclusive);
        if (!(unit instanceof ChronoUnit)) {
            return unit.between(this, end);
        }
        if (!isSupported(unit)) {
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        return amount(Duration.ofSeconds(end.seconds - seconds, end.nanos - nanos), unit);
    }

    public OffsetDateTime atOffset(ZoneOffset offset) {
        return OffsetDateTime.ofInstant(this, offset);
    }

    public ZonedDateTime atZone(ZoneId zone) {
        return ZonedDateTime.ofInstant(this, zone);
    }

    public long toEpochMilli() {
        if (seconds < 0 && nanos > 0) {
            long millis = Math.multiplyExact(seconds + 1, 1000);
            long adjustment = nanos / 1_000_000 - 1000;
            return Math.addExact(millis, adjustment);
        }
        long millis = Math.multiplyExact(seconds, 1000);
        return Math.addExact(millis, nanos / 1_000_000);
    }

    @Override
    public int compareTo(Instant otherInstant) {
        int cmp = Long.compare(seconds, otherInstant.seconds);
        return cmp != 0 ? cmp : nanos - otherInstant.nanos;
    }

    public boolean isAfter(Instant otherInstant) {
        return compareTo(otherInstant) > 0;
    }

    public boolean isBefore(Instant otherInstant) {
        return compareTo(otherInstant) < 0;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Instant instant && seconds == instant.seconds
                && nanos == instant.nanos;
    }

    @Override
    public int hashCode() {
        return ((int) (seconds ^ (seconds >>> 32))) + 51 * nanos;
    }

    @Override
    public String toString() {
        return DateTimeFormatter.ISO_INSTANT.format(this);
    }
}
