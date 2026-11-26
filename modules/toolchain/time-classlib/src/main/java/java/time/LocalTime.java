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

public final class LocalTime implements Temporal, TemporalAdjuster, Comparable<LocalTime> {

    public static final LocalTime MIN = new LocalTime(0, 0, 0, 0);
    public static final LocalTime MAX = new LocalTime(23, 59, 59, 999_999_999);
    public static final LocalTime MIDNIGHT = MIN;
    public static final LocalTime NOON = new LocalTime(12, 0, 0, 0);

    static final long NANOS_PER_SECOND = 1_000_000_000L;
    static final long NANOS_PER_MINUTE = NANOS_PER_SECOND * 60;
    static final long NANOS_PER_HOUR = NANOS_PER_MINUTE * 60;
    static final long NANOS_PER_DAY = NANOS_PER_HOUR * 24;

    private final byte hour;
    private final byte minute;
    private final byte second;
    private final int nano;

    private LocalTime(int hour, int minute, int second, int nanoOfSecond) {
        this.hour = (byte) hour;
        this.minute = (byte) minute;
        this.second = (byte) second;
        this.nano = nanoOfSecond;
    }

    public static LocalTime now() {
        return now(Clock.systemDefaultZone());
    }

    public static LocalTime now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    public static LocalTime now(Clock clock) {
        return ofInstant(clock.instant(), clock.getZone());
    }

    public static LocalTime of(int hour, int minute) {
        return of(hour, minute, 0, 0);
    }

    public static LocalTime of(int hour, int minute, int second) {
        return of(hour, minute, second, 0);
    }

    public static LocalTime of(int hour, int minute, int second, int nanoOfSecond) {
        ChronoField.HOUR_OF_DAY.checkValidValue(hour);
        ChronoField.MINUTE_OF_HOUR.checkValidValue(minute);
        ChronoField.SECOND_OF_MINUTE.checkValidValue(second);
        ChronoField.NANO_OF_SECOND.checkValidValue(nanoOfSecond);
        return new LocalTime(hour, minute, second, nanoOfSecond);
    }

    public static LocalTime ofInstant(Instant instant, ZoneId zone) {
        ZoneOffset offset = zone.getRules().getOffset(instant);
        long localSecond = instant.getEpochSecond() + offset.getTotalSeconds();
        int secsOfDay = (int) Math.floorMod(localSecond, 86400L);
        return ofNanoOfDay(secsOfDay * NANOS_PER_SECOND + instant.getNano());
    }

    public static LocalTime ofSecondOfDay(long secondOfDay) {
        ChronoField.SECOND_OF_DAY.checkValidValue(secondOfDay);
        int hours = (int) (secondOfDay / 3600);
        secondOfDay -= hours * 3600L;
        int minutes = (int) (secondOfDay / 60);
        secondOfDay -= minutes * 60L;
        return new LocalTime(hours, minutes, (int) secondOfDay, 0);
    }

    public static LocalTime ofNanoOfDay(long nanoOfDay) {
        ChronoField.NANO_OF_DAY.checkValidValue(nanoOfDay);
        int hours = (int) (nanoOfDay / NANOS_PER_HOUR);
        nanoOfDay -= hours * NANOS_PER_HOUR;
        int minutes = (int) (nanoOfDay / NANOS_PER_MINUTE);
        nanoOfDay -= minutes * NANOS_PER_MINUTE;
        int seconds = (int) (nanoOfDay / NANOS_PER_SECOND);
        nanoOfDay -= seconds * NANOS_PER_SECOND;
        return new LocalTime(hours, minutes, seconds, (int) nanoOfDay);
    }

    public static LocalTime from(TemporalAccessor temporal) {
        LocalTime time = temporal.query(TemporalQueries.localTime());
        if (time == null) {
            throw new DateTimeException("Unable to obtain LocalTime from TemporalAccessor: "
                    + temporal + " of type " + temporal.getClass().getName());
        }
        return time;
    }

    public static LocalTime parse(CharSequence text) {
        return parse(text, DateTimeFormatter.ISO_LOCAL_TIME);
    }

    public static LocalTime parse(CharSequence text, DateTimeFormatter formatter) {
        return formatter.parse(text, LocalTime::from);
    }

    @Override
    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField chrono) {
            return chrono.isTimeBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isTimeBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public int get(TemporalField field) {
        return (int) getLong(field);
    }

    @Override
    public long getLong(TemporalField field) {
        if (!(field instanceof ChronoField chrono)) {
            return field.getFrom(this);
        }
        switch (chrono) {
            case NANO_OF_SECOND:
                return nano;
            case NANO_OF_DAY:
                return toNanoOfDay();
            case MICRO_OF_SECOND:
                return nano / 1000;
            case MICRO_OF_DAY:
                return toNanoOfDay() / 1000;
            case MILLI_OF_SECOND:
                return nano / 1_000_000;
            case MILLI_OF_DAY:
                return toNanoOfDay() / 1_000_000;
            case SECOND_OF_MINUTE:
                return second;
            case SECOND_OF_DAY:
                return toSecondOfDay();
            case MINUTE_OF_HOUR:
                return minute;
            case MINUTE_OF_DAY:
                return hour * 60 + minute;
            case HOUR_OF_AMPM:
                return hour % 12;
            case CLOCK_HOUR_OF_AMPM:
                return hour % 12 == 0 ? 12 : hour % 12;
            case HOUR_OF_DAY:
                return hour;
            case CLOCK_HOUR_OF_DAY:
                return hour == 0 ? 24 : hour;
            case AMPM_OF_DAY:
                return hour / 12;
            default:
                throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSecond() {
        return second;
    }

    public int getNano() {
        return nano;
    }

    @Override
    public LocalTime with(TemporalAdjuster adjuster) {
        if (adjuster instanceof LocalTime time) {
            return time;
        }
        return (LocalTime) adjuster.adjustInto(this);
    }

    @Override
    public LocalTime with(TemporalField field, long newValue) {
        if (!(field instanceof ChronoField chrono)) {
            return field.adjustInto(this, newValue);
        }
        chrono.checkValidValue(newValue);
        switch (chrono) {
            case NANO_OF_SECOND:
                return withNano((int) newValue);
            case NANO_OF_DAY:
                return ofNanoOfDay(newValue);
            case MICRO_OF_SECOND:
                return withNano((int) newValue * 1000);
            case MICRO_OF_DAY:
                return ofNanoOfDay(newValue * 1000);
            case MILLI_OF_SECOND:
                return withNano((int) newValue * 1_000_000);
            case MILLI_OF_DAY:
                return ofNanoOfDay(newValue * 1_000_000);
            case SECOND_OF_MINUTE:
                return withSecond((int) newValue);
            case SECOND_OF_DAY:
                return plusSeconds(newValue - toSecondOfDay());
            case MINUTE_OF_HOUR:
                return withMinute((int) newValue);
            case MINUTE_OF_DAY:
                return plusMinutes(newValue - (hour * 60 + minute));
            case HOUR_OF_AMPM:
                return plusHours(newValue - (hour % 12));
            case CLOCK_HOUR_OF_AMPM:
                return plusHours((newValue == 12 ? 0 : newValue) - (hour % 12));
            case HOUR_OF_DAY:
                return withHour((int) newValue);
            case CLOCK_HOUR_OF_DAY:
                return withHour((int) (newValue == 24 ? 0 : newValue));
            case AMPM_OF_DAY:
                return plusHours((newValue - (hour / 12)) * 12);
            default:
                throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
    }

    public LocalTime withHour(int hour) {
        return this.hour == hour ? this : of(hour, minute, second, nano);
    }

    public LocalTime withMinute(int minute) {
        return this.minute == minute ? this : of(hour, minute, second, nano);
    }

    public LocalTime withSecond(int second) {
        return this.second == second ? this : of(hour, minute, second, nano);
    }

    public LocalTime withNano(int nanoOfSecond) {
        return nano == nanoOfSecond ? this : of(hour, minute, second, nanoOfSecond);
    }

    public LocalTime truncatedTo(TemporalUnit unit) {
        if (unit == ChronoUnit.NANOS) {
            return this;
        }
        Duration unitDur = unit.getDuration();
        if (unitDur.getSeconds() > 86400) {
            throw new UnsupportedTemporalTypeException(
                    "Unit is too large to be used for truncation");
        }
        long dur = unitDur.toNanos();
        if ((NANOS_PER_DAY % dur) != 0) {
            throw new UnsupportedTemporalTypeException(
                    "Unit must divide into a standard day without remainder");
        }
        long nod = toNanoOfDay();
        return ofNanoOfDay((nod / dur) * dur);
    }

    @Override
    public LocalTime plus(TemporalAmount amountToAdd) {
        return (LocalTime) amountToAdd.addTo(this);
    }

    @Override
    public LocalTime plus(long amountToAdd, TemporalUnit unit) {
        if (!(unit instanceof ChronoUnit chrono)) {
            return unit.addTo(this, amountToAdd);
        }
        switch (chrono) {
            case NANOS:
                return plusNanos(amountToAdd);
            case MICROS:
                return plusNanos((amountToAdd % (NANOS_PER_DAY / 1000)) * 1000);
            case MILLIS:
                return plusNanos((amountToAdd % (NANOS_PER_DAY / 1_000_000)) * 1_000_000);
            case SECONDS:
                return plusSeconds(amountToAdd);
            case MINUTES:
                return plusMinutes(amountToAdd);
            case HOURS:
                return plusHours(amountToAdd);
            case HALF_DAYS:
                return plusHours((amountToAdd % 2) * 12);
            default:
                throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
    }

    public LocalTime plusHours(long hoursToAdd) {
        if (hoursToAdd == 0) {
            return this;
        }
        int newHour = ((int) (hoursToAdd % 24) + hour + 24) % 24;
        return new LocalTime(newHour, minute, second, nano);
    }

    public LocalTime plusMinutes(long minutesToAdd) {
        if (minutesToAdd == 0) {
            return this;
        }
        int mofd = hour * 60 + minute;
        int newMofd = ((int) (minutesToAdd % 1440) + mofd + 1440) % 1440;
        if (mofd == newMofd) {
            return this;
        }
        return new LocalTime(newMofd / 60, newMofd % 60, second, nano);
    }

    public LocalTime plusSeconds(long secondsToAdd) {
        if (secondsToAdd == 0) {
            return this;
        }
        int sofd = toSecondOfDay();
        int newSofd = ((int) (secondsToAdd % 86400) + sofd + 86400) % 86400;
        if (sofd == newSofd) {
            return this;
        }
        return new LocalTime(newSofd / 3600, (newSofd / 60) % 60, newSofd % 60, nano);
    }

    public LocalTime plusNanos(long nanosToAdd) {
        if (nanosToAdd == 0) {
            return this;
        }
        long nofd = toNanoOfDay();
        long newNofd = ((nanosToAdd % NANOS_PER_DAY) + nofd + NANOS_PER_DAY) % NANOS_PER_DAY;
        return nofd == newNofd ? this : ofNanoOfDay(newNofd);
    }

    @Override
    public LocalTime minus(TemporalAmount amountToSubtract) {
        return (LocalTime) amountToSubtract.subtractFrom(this);
    }

    @Override
    public LocalTime minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE
                ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit);
    }

    public LocalTime minusHours(long hoursToSubtract) {
        return plusHours(-(hoursToSubtract % 24));
    }

    public LocalTime minusMinutes(long minutesToSubtract) {
        return plusMinutes(-(minutesToSubtract % 1440));
    }

    public LocalTime minusSeconds(long secondsToSubtract) {
        return plusSeconds(-(secondsToSubtract % 86400));
    }

    public LocalTime minusNanos(long nanosToSubtract) {
        return plusNanos(-(nanosToSubtract % NANOS_PER_DAY));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.localTime()) {
            return (R) this;
        }
        if (query == TemporalQueries.precision()) {
            return (R) ChronoUnit.NANOS;
        }
        return Temporal.super.query(query);
    }

    @Override
    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.NANO_OF_DAY, toNanoOfDay());
    }

    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        LocalTime end = LocalTime.from(endExclusive);
        if (!(unit instanceof ChronoUnit chrono)) {
            return unit.between(this, end);
        }
        long nanosUntil = end.toNanoOfDay() - toNanoOfDay();
        switch (chrono) {
            case NANOS:
                return nanosUntil;
            case MICROS:
                return nanosUntil / 1000;
            case MILLIS:
                return nanosUntil / 1_000_000;
            case SECONDS:
                return nanosUntil / NANOS_PER_SECOND;
            case MINUTES:
                return nanosUntil / NANOS_PER_MINUTE;
            case HOURS:
                return nanosUntil / NANOS_PER_HOUR;
            case HALF_DAYS:
                return nanosUntil / (12 * NANOS_PER_HOUR);
            default:
                throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    public LocalDateTime atDate(LocalDate date) {
        return LocalDateTime.of(date, this);
    }

    public OffsetDateTime atOffset(ZoneOffset offset) {
        return OffsetDateTime.of(LocalDate.EPOCH, this, offset);
    }

    public int toSecondOfDay() {
        return hour * 3600 + minute * 60 + second;
    }

    public long toNanoOfDay() {
        return hour * NANOS_PER_HOUR + minute * NANOS_PER_MINUTE + second * NANOS_PER_SECOND + nano;
    }

    public long toEpochSecond(LocalDate date, ZoneOffset offset) {
        return date.toEpochDay() * 86400 + toSecondOfDay() - offset.getTotalSeconds();
    }

    @Override
    public int compareTo(LocalTime other) {
        int cmp = Integer.compare(hour, other.hour);
        if (cmp == 0) {
            cmp = Integer.compare(minute, other.minute);
            if (cmp == 0) {
                cmp = Integer.compare(second, other.second);
                if (cmp == 0) {
                    cmp = Integer.compare(nano, other.nano);
                }
            }
        }
        return cmp;
    }

    public boolean isAfter(LocalTime other) {
        return compareTo(other) > 0;
    }

    public boolean isBefore(LocalTime other) {
        return compareTo(other) < 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LocalTime other && compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(toNanoOfDay());
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(18);
        buf.append(hour < 10 ? "0" : "").append(hour)
                .append(minute < 10 ? ":0" : ":").append(minute);
        if (second > 0 || nano > 0) {
            buf.append(second < 10 ? ":0" : ":").append(second);
            if (nano > 0) {
                buf.append('.');
                if (nano % 1_000_000 == 0) {
                    buf.append(Integer.toString((nano / 1_000_000) + 1000).substring(1));
                } else if (nano % 1000 == 0) {
                    buf.append(Integer.toString((nano / 1000) + 1_000_000).substring(1));
                } else {
                    buf.append(Integer.toString((nano) + 1_000_000_000).substring(1));
                }
            }
        }
        return buf.toString();
    }
}
