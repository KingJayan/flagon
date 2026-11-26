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
import java.time.temporal.ValueRange;

public final class OffsetDateTime implements Temporal, TemporalAdjuster,
        Comparable<OffsetDateTime> {

    public static final OffsetDateTime MIN = LocalDateTime.MIN.atOffset(ZoneOffset.MAX);
    public static final OffsetDateTime MAX = LocalDateTime.MAX.atOffset(ZoneOffset.MIN);

    private final LocalDateTime dateTime;
    private final ZoneOffset offset;

    private OffsetDateTime(LocalDateTime dateTime, ZoneOffset offset) {
        this.dateTime = dateTime;
        this.offset = offset;
    }

    public static OffsetDateTime now() {
        return now(Clock.systemDefaultZone());
    }

    public static OffsetDateTime now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    public static OffsetDateTime now(Clock clock) {
        return ofInstant(clock.instant(), clock.getZone());
    }

    public static OffsetDateTime of(LocalDate date, LocalTime time, ZoneOffset offset) {
        return new OffsetDateTime(LocalDateTime.of(date, time), offset);
    }

    public static OffsetDateTime of(LocalDateTime dateTime, ZoneOffset offset) {
        if (dateTime == null || offset == null) {
            throw new NullPointerException(dateTime == null ? "dateTime" : "offset");
        }
        return new OffsetDateTime(dateTime, offset);
    }

    public static OffsetDateTime of(int year, int month, int dayOfMonth, int hour, int minute,
            int second, int nanoOfSecond, ZoneOffset offset) {
        return new OffsetDateTime(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second,
                nanoOfSecond), offset);
    }

    public static OffsetDateTime ofInstant(Instant instant, ZoneId zone) {
        ZoneOffset offset = zone.getRules().getOffset(instant);
        return new OffsetDateTime(LocalDateTime.ofEpochSecond(instant.getEpochSecond(),
                instant.getNano(), offset), offset);
    }

    public static OffsetDateTime from(TemporalAccessor temporal) {
        if (temporal instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        try {
            ZoneOffset offset = ZoneOffset.from(temporal);
            LocalDate date = temporal.query(TemporalQueries.localDate());
            LocalTime time = temporal.query(TemporalQueries.localTime());
            if (date != null && time != null) {
                return of(date, time, offset);
            }
            return ofInstant(Instant.from(temporal), offset);
        } catch (DateTimeException e) {
            throw new DateTimeException("Unable to obtain OffsetDateTime from TemporalAccessor: "
                    + temporal + " of type " + temporal.getClass().getName(), e);
        }
    }

    public static OffsetDateTime parse(CharSequence text) {
        return parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static OffsetDateTime parse(CharSequence text, DateTimeFormatter formatter) {
        return formatter.parse(text, OffsetDateTime::from);
    }

    private OffsetDateTime with(LocalDateTime newDateTime, ZoneOffset newOffset) {
        if (dateTime == newDateTime && offset.equals(newOffset)) {
            return this;
        }
        return new OffsetDateTime(newDateTime, newOffset);
    }

    @Override
    public boolean isSupported(TemporalField field) {
        return field instanceof ChronoField || field != null && field.isSupportedBy(this);
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        return dateTime.isSupported(unit);
    }

    @Override
    public ValueRange range(TemporalField field) {
        if (field == ChronoField.INSTANT_SECONDS || field == ChronoField.OFFSET_SECONDS) {
            return field.range();
        }
        return dateTime.range(field);
    }

    @Override
    public int get(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return offset.getTotalSeconds();
        }
        return Temporal.super.get(field);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field == ChronoField.INSTANT_SECONDS) {
            return toEpochSecond();
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            return offset.getTotalSeconds();
        }
        return dateTime.getLong(field);
    }

    public ZoneOffset getOffset() {
        return offset;
    }

    public OffsetDateTime withOffsetSameLocal(ZoneOffset offset) {
        return with(dateTime, offset);
    }

    public OffsetDateTime withOffsetSameInstant(ZoneOffset offset) {
        if (offset.equals(this.offset)) {
            return this;
        }
        int difference = offset.getTotalSeconds() - this.offset.getTotalSeconds();
        return new OffsetDateTime(dateTime.plusSeconds(difference), offset);
    }

    public LocalDateTime toLocalDateTime() {
        return dateTime;
    }

    public LocalDate toLocalDate() {
        return dateTime.toLocalDate();
    }

    public LocalTime toLocalTime() {
        return dateTime.toLocalTime();
    }

    public int getYear() {
        return dateTime.getYear();
    }

    public int getMonthValue() {
        return dateTime.getMonthValue();
    }

    public Month getMonth() {
        return dateTime.getMonth();
    }

    public int getDayOfMonth() {
        return dateTime.getDayOfMonth();
    }

    public int getDayOfYear() {
        return dateTime.getDayOfYear();
    }

    public DayOfWeek getDayOfWeek() {
        return dateTime.getDayOfWeek();
    }

    public int getHour() {
        return dateTime.getHour();
    }

    public int getMinute() {
        return dateTime.getMinute();
    }

    public int getSecond() {
        return dateTime.getSecond();
    }

    public int getNano() {
        return dateTime.getNano();
    }

    @Override
    public OffsetDateTime with(TemporalAdjuster adjuster) {
        if (adjuster instanceof LocalDate || adjuster instanceof LocalTime
                || adjuster instanceof LocalDateTime) {
            return with(dateTime.with(adjuster), offset);
        }
        if (adjuster instanceof Instant instant) {
            return ofInstant(instant, offset);
        }
        if (adjuster instanceof ZoneOffset newOffset) {
            return with(dateTime, newOffset);
        }
        return (OffsetDateTime) adjuster.adjustInto(this);
    }

    @Override
    public OffsetDateTime with(TemporalField field, long newValue) {
        if (field instanceof ChronoField chrono) {
            switch (chrono) {
                case INSTANT_SECONDS:
                    return ofInstant(Instant.ofEpochSecond(newValue, getNano()), offset);
                case OFFSET_SECONDS:
                    int seconds = chrono.checkValidIntValue(newValue);
                    return with(dateTime, ZoneOffset.ofTotalSeconds(seconds));
                default:
                    return with(dateTime.with(field, newValue), offset);
            }
        }
        return field.adjustInto(this, newValue);
    }

    public OffsetDateTime truncatedTo(TemporalUnit unit) {
        return with(dateTime.truncatedTo(unit), offset);
    }

    @Override
    public OffsetDateTime plus(TemporalAmount amountToAdd) {
        return (OffsetDateTime) amountToAdd.addTo(this);
    }

    @Override
    public OffsetDateTime plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return with(dateTime.plus(amountToAdd, unit), offset);
        }
        return unit.addTo(this, amountToAdd);
    }

    public OffsetDateTime plusYears(long years) {
        return with(dateTime.plusYears(years), offset);
    }

    public OffsetDateTime plusMonths(long months) {
        return with(dateTime.plusMonths(months), offset);
    }

    public OffsetDateTime plusWeeks(long weeks) {
        return with(dateTime.plusWeeks(weeks), offset);
    }

    public OffsetDateTime plusDays(long days) {
        return with(dateTime.plusDays(days), offset);
    }

    public OffsetDateTime plusHours(long hours) {
        return with(dateTime.plusHours(hours), offset);
    }

    public OffsetDateTime plusMinutes(long minutes) {
        return with(dateTime.plusMinutes(minutes), offset);
    }

    public OffsetDateTime plusSeconds(long seconds) {
        return with(dateTime.plusSeconds(seconds), offset);
    }

    public OffsetDateTime plusNanos(long nanos) {
        return with(dateTime.plusNanos(nanos), offset);
    }

    @Override
    public OffsetDateTime minus(TemporalAmount amountToSubtract) {
        return (OffsetDateTime) amountToSubtract.subtractFrom(this);
    }

    @Override
    public OffsetDateTime minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE
                ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit);
    }

    public OffsetDateTime minusYears(long years) {
        return plusYears(-years);
    }

    public OffsetDateTime minusMonths(long months) {
        return plusMonths(-months);
    }

    public OffsetDateTime minusWeeks(long weeks) {
        return plusWeeks(-weeks);
    }

    public OffsetDateTime minusDays(long days) {
        return plusDays(-days);
    }

    public OffsetDateTime minusHours(long hours) {
        return plusHours(-hours);
    }

    public OffsetDateTime minusMinutes(long minutes) {
        return plusMinutes(-minutes);
    }

    public OffsetDateTime minusSeconds(long seconds) {
        return plusSeconds(-seconds);
    }

    public OffsetDateTime minusNanos(long nanos) {
        return plusNanos(-nanos);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.offset() || query == TemporalQueries.zone()) {
            return (R) offset;
        }
        if (query == TemporalQueries.zoneId()) {
            return null;
        }
        if (query == TemporalQueries.localDate()) {
            return (R) toLocalDate();
        }
        if (query == TemporalQueries.localTime()) {
            return (R) toLocalTime();
        }
        if (query == TemporalQueries.precision()) {
            return (R) ChronoUnit.NANOS;
        }
        return Temporal.super.query(query);
    }

    @Override
    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.EPOCH_DAY, toLocalDate().toEpochDay())
                .with(ChronoField.NANO_OF_DAY, toLocalTime().toNanoOfDay())
                .with(ChronoField.OFFSET_SECONDS, offset.getTotalSeconds());
    }

    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        OffsetDateTime end = OffsetDateTime.from(endExclusive);
        if (unit instanceof ChronoUnit) {
            return dateTime.until(end.withOffsetSameInstant(offset).dateTime, unit);
        }
        return unit.between(this, end);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    public ZonedDateTime atZoneSameInstant(ZoneId zone) {
        return ZonedDateTime.ofInstant(dateTime, offset, zone);
    }

    public ZonedDateTime atZoneSimilarLocal(ZoneId zone) {
        return ZonedDateTime.ofLocal(dateTime, zone, offset);
    }

    public ZonedDateTime toZonedDateTime() {
        return ZonedDateTime.of(dateTime, offset);
    }

    public long toEpochSecond() {
        return dateTime.toEpochSecond(offset);
    }

    public Instant toInstant() {
        return dateTime.toInstant(offset);
    }

    @Override
    public int compareTo(OffsetDateTime other) {
        int cmp = Long.compare(toEpochSecond(), other.toEpochSecond());
        if (cmp == 0) {
            cmp = getNano() - other.getNano();
        }
        return cmp == 0 ? dateTime.compareTo(other.dateTime) : cmp;
    }

    public boolean isAfter(OffsetDateTime other) {
        long thisEpochSec = toEpochSecond();
        long otherEpochSec = other.toEpochSecond();
        return thisEpochSec > otherEpochSec
                || (thisEpochSec == otherEpochSec && getNano() > other.getNano());
    }

    public boolean isBefore(OffsetDateTime other) {
        long thisEpochSec = toEpochSecond();
        long otherEpochSec = other.toEpochSecond();
        return thisEpochSec < otherEpochSec
                || (thisEpochSec == otherEpochSec && getNano() < other.getNano());
    }

    public boolean isEqual(OffsetDateTime other) {
        return toEpochSecond() == other.toEpochSecond() && getNano() == other.getNano();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof OffsetDateTime other && dateTime.equals(other.dateTime)
                && offset.equals(other.offset);
    }

    @Override
    public int hashCode() {
        return dateTime.hashCode() ^ offset.hashCode();
    }

    @Override
    public String toString() {
        return dateTime.toString() + offset.toString();
    }
}
