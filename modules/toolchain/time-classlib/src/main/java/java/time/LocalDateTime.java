package java.time;

import java.time.chrono.ChronoLocalDateTime;
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

public final class LocalDateTime
        implements Temporal, TemporalAdjuster, ChronoLocalDateTime<LocalDate> {

    public static final LocalDateTime MIN = new LocalDateTime(LocalDate.MIN, LocalTime.MIN);
    public static final LocalDateTime MAX = new LocalDateTime(LocalDate.MAX, LocalTime.MAX);

    private final LocalDate date;
    private final LocalTime time;

    private LocalDateTime(LocalDate date, LocalTime time) {
        this.date = date;
        this.time = time;
    }

    public static LocalDateTime now() {
        return now(Clock.systemDefaultZone());
    }

    public static LocalDateTime now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    public static LocalDateTime now(Clock clock) {
        return ofInstant(clock.instant(), clock.getZone());
    }

    public static LocalDateTime of(int year, Month month, int dayOfMonth, int hour, int minute) {
        return of(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute));
    }

    public static LocalDateTime of(int year, Month month, int dayOfMonth, int hour, int minute,
            int second) {
        return of(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute, second));
    }

    public static LocalDateTime of(int year, Month month, int dayOfMonth, int hour, int minute,
            int second, int nanoOfSecond) {
        return of(LocalDate.of(year, month, dayOfMonth),
                LocalTime.of(hour, minute, second, nanoOfSecond));
    }

    public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute) {
        return of(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute));
    }

    public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute,
            int second) {
        return of(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute, second));
    }

    public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute,
            int second, int nanoOfSecond) {
        return of(LocalDate.of(year, month, dayOfMonth),
                LocalTime.of(hour, minute, second, nanoOfSecond));
    }

    public static LocalDateTime of(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            throw new NullPointerException(date == null ? "date" : "time");
        }
        return new LocalDateTime(date, time);
    }

    public static LocalDateTime ofInstant(Instant instant, ZoneId zone) {
        ZoneOffset offset = zone.getRules().getOffset(instant);
        return ofEpochSecond(instant.getEpochSecond(), instant.getNano(), offset);
    }

    public static LocalDateTime ofEpochSecond(long epochSecond, int nanoOfSecond,
            ZoneOffset offset) {
        ChronoField.NANO_OF_SECOND.checkValidValue(nanoOfSecond);
        long localSecond = epochSecond + offset.getTotalSeconds();
        long localEpochDay = Math.floorDiv(localSecond, 86400L);
        int secsOfDay = (int) Math.floorMod(localSecond, 86400L);
        LocalDate date = LocalDate.ofEpochDay(localEpochDay);
        LocalTime time = LocalTime.ofNanoOfDay(secsOfDay * LocalTime.NANOS_PER_SECOND
                + nanoOfSecond);
        return new LocalDateTime(date, time);
    }

    public static LocalDateTime from(TemporalAccessor temporal) {
        if (temporal instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (temporal instanceof ZonedDateTime zoned) {
            return zoned.toLocalDateTime();
        }
        if (temporal instanceof OffsetDateTime offset) {
            return offset.toLocalDateTime();
        }
        LocalDate date = temporal.query(TemporalQueries.localDate());
        LocalTime time = temporal.query(TemporalQueries.localTime());
        if (date == null || time == null) {
            throw new DateTimeException("Unable to obtain LocalDateTime from TemporalAccessor: "
                    + temporal + " of type " + temporal.getClass().getName());
        }
        return new LocalDateTime(date, time);
    }

    public static LocalDateTime parse(CharSequence text) {
        return parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static LocalDateTime parse(CharSequence text, DateTimeFormatter formatter) {
        return formatter.parse(text, LocalDateTime::from);
    }

    private LocalDateTime with(LocalDate newDate, LocalTime newTime) {
        if (date == newDate && time == newTime) {
            return this;
        }
        return new LocalDateTime(newDate, newTime);
    }

    @Override
    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField chrono) {
            return chrono.isDateBased() || chrono.isTimeBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit != ChronoUnit.FOREVER;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public ValueRange range(TemporalField field) {
        if (field instanceof ChronoField chrono) {
            return chrono.isTimeBased() ? time.range(field) : date.range(field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public int get(TemporalField field) {
        if (field instanceof ChronoField chrono) {
            return chrono.isTimeBased() ? time.get(field) : date.get(field);
        }
        return ChronoLocalDateTime.super.get(field);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field instanceof ChronoField chrono) {
            return chrono.isTimeBased() ? time.getLong(field) : date.getLong(field);
        }
        return field.getFrom(this);
    }

    public LocalDate toLocalDate() {
        return date;
    }

    public LocalTime toLocalTime() {
        return time;
    }

    public int getYear() {
        return date.getYear();
    }

    public int getMonthValue() {
        return date.getMonthValue();
    }

    public Month getMonth() {
        return date.getMonth();
    }

    public int getDayOfMonth() {
        return date.getDayOfMonth();
    }

    public int getDayOfYear() {
        return date.getDayOfYear();
    }

    public DayOfWeek getDayOfWeek() {
        return date.getDayOfWeek();
    }

    public int getHour() {
        return time.getHour();
    }

    public int getMinute() {
        return time.getMinute();
    }

    public int getSecond() {
        return time.getSecond();
    }

    public int getNano() {
        return time.getNano();
    }

    @Override
    public LocalDateTime with(TemporalAdjuster adjuster) {
        if (adjuster instanceof LocalDate newDate) {
            return with(newDate, time);
        }
        if (adjuster instanceof LocalTime newTime) {
            return with(date, newTime);
        }
        if (adjuster instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        return (LocalDateTime) adjuster.adjustInto(this);
    }

    @Override
    public LocalDateTime with(TemporalField field, long newValue) {
        if (field instanceof ChronoField chrono) {
            return chrono.isTimeBased() ? with(date, time.with(field, newValue))
                    : with(date.with(field, newValue), time);
        }
        return field.adjustInto(this, newValue);
    }

    public LocalDateTime withYear(int year) {
        return with(date.withYear(year), time);
    }

    public LocalDateTime withMonth(int month) {
        return with(date.withMonth(month), time);
    }

    public LocalDateTime withDayOfMonth(int dayOfMonth) {
        return with(date.withDayOfMonth(dayOfMonth), time);
    }

    public LocalDateTime withDayOfYear(int dayOfYear) {
        return with(date.withDayOfYear(dayOfYear), time);
    }

    public LocalDateTime withHour(int hour) {
        return with(date, time.withHour(hour));
    }

    public LocalDateTime withMinute(int minute) {
        return with(date, time.withMinute(minute));
    }

    public LocalDateTime withSecond(int second) {
        return with(date, time.withSecond(second));
    }

    public LocalDateTime withNano(int nanoOfSecond) {
        return with(date, time.withNano(nanoOfSecond));
    }

    public LocalDateTime truncatedTo(TemporalUnit unit) {
        return with(date, time.truncatedTo(unit));
    }

    @Override
    public LocalDateTime plus(TemporalAmount amountToAdd) {
        if (amountToAdd instanceof Period period) {
            return with(date.plus(period), time);
        }
        return (LocalDateTime) amountToAdd.addTo(this);
    }

    @Override
    public LocalDateTime plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit chrono) {
            switch (chrono) {
                case NANOS:
                    return plusNanos(amountToAdd);
                case MICROS:
                    return plusDays(amountToAdd / 86_400_000_000L)
                            .plusNanos((amountToAdd % 86_400_000_000L) * 1000);
                case MILLIS:
                    return plusDays(amountToAdd / 86_400_000L)
                            .plusNanos((amountToAdd % 86_400_000L) * 1_000_000);
                case SECONDS:
                    return plusSeconds(amountToAdd);
                case MINUTES:
                    return plusMinutes(amountToAdd);
                case HOURS:
                    return plusHours(amountToAdd);
                case HALF_DAYS:
                    return plusDays(amountToAdd / 256).plusHours((amountToAdd % 256) * 12);
                default:
                    return with(date.plus(amountToAdd, unit), time);
            }
        }
        return unit.addTo(this, amountToAdd);
    }

    public LocalDateTime plusYears(long years) {
        return with(date.plusYears(years), time);
    }

    public LocalDateTime plusMonths(long months) {
        return with(date.plusMonths(months), time);
    }

    public LocalDateTime plusWeeks(long weeks) {
        return with(date.plusWeeks(weeks), time);
    }

    public LocalDateTime plusDays(long days) {
        return with(date.plusDays(days), time);
    }

    public LocalDateTime plusHours(long hours) {
        return plusWithOverflow((hours % 24) * LocalTime.NANOS_PER_HOUR, hours / 24);
    }

    public LocalDateTime plusMinutes(long minutes) {
        return plusWithOverflow((minutes % 1440) * LocalTime.NANOS_PER_MINUTE, minutes / 1440);
    }

    public LocalDateTime plusSeconds(long seconds) {
        return plusWithOverflow((seconds % 86400) * LocalTime.NANOS_PER_SECOND, seconds / 86400);
    }

    public LocalDateTime plusNanos(long nanos) {
        return plusWithOverflow(nanos % LocalTime.NANOS_PER_DAY, nanos / LocalTime.NANOS_PER_DAY);
    }

    /** adds whole days, then nanos less than a day, carrying into the date */
    private LocalDateTime plusWithOverflow(long nanos, long days) {
        if (nanos == 0 && days == 0) {
            return this;
        }
        nanos %= LocalTime.NANOS_PER_DAY;
        long curNoD = time.toNanoOfDay();
        long totNanos = nanos + curNoD;
        long totDays = days + Math.floorDiv(totNanos, LocalTime.NANOS_PER_DAY);
        long newNoD = Math.floorMod(totNanos, LocalTime.NANOS_PER_DAY);
        LocalTime newTime = newNoD == curNoD ? time : LocalTime.ofNanoOfDay(newNoD);
        return with(date.plusDays(totDays), newTime);
    }

    @Override
    public LocalDateTime minus(TemporalAmount amountToSubtract) {
        if (amountToSubtract instanceof Period period) {
            return with(date.minus(period), time);
        }
        return (LocalDateTime) amountToSubtract.subtractFrom(this);
    }

    @Override
    public LocalDateTime minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE
                ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit);
    }

    public LocalDateTime minusYears(long years) {
        return plusYears(-years);
    }

    public LocalDateTime minusMonths(long months) {
        return plusMonths(-months);
    }

    public LocalDateTime minusWeeks(long weeks) {
        return plusWeeks(-weeks);
    }

    public LocalDateTime minusDays(long days) {
        return plusDays(-days);
    }

    public LocalDateTime minusHours(long hours) {
        return plusHours(-hours);
    }

    public LocalDateTime minusMinutes(long minutes) {
        return plusMinutes(-minutes);
    }

    public LocalDateTime minusSeconds(long seconds) {
        return plusSeconds(-seconds);
    }

    public LocalDateTime minusNanos(long nanos) {
        return plusNanos(-nanos);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.localDate()) {
            return (R) date;
        }
        if (query == TemporalQueries.localTime()) {
            return (R) time;
        }
        if (query == TemporalQueries.precision()) {
            return (R) ChronoUnit.NANOS;
        }
        return ChronoLocalDateTime.super.query(query);
    }

    @Override
    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.EPOCH_DAY, date.toEpochDay())
                .with(ChronoField.NANO_OF_DAY, time.toNanoOfDay());
    }

    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        LocalDateTime end = LocalDateTime.from(endExclusive);
        if (unit.isTimeBased()) {
            long seconds = (end.date.toEpochDay() - date.toEpochDay()) * 86400
                    + (end.time.toSecondOfDay() - time.toSecondOfDay());
            long nanos = end.time.getNano() - time.getNano();
            return Instant.amount(Duration.ofSeconds(seconds, nanos), unit);
        }
        LocalDate endDate = end.date;
        if (endDate.isAfter(date) && end.time.isBefore(time)) {
            endDate = endDate.minusDays(1);
        } else if (endDate.isBefore(date) && end.time.isAfter(time)) {
            endDate = endDate.plusDays(1);
        }
        return date.until(endDate, unit);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    public OffsetDateTime atOffset(ZoneOffset offset) {
        return OffsetDateTime.of(this, offset);
    }

    public ZonedDateTime atZone(ZoneId zone) {
        return ZonedDateTime.of(this, zone);
    }

    public long toEpochSecond(ZoneOffset offset) {
        return date.toEpochDay() * 86400 + time.toSecondOfDay() - offset.getTotalSeconds();
    }

    public Instant toInstant(ZoneOffset offset) {
        return Instant.ofEpochSecond(toEpochSecond(offset), time.getNano());
    }

    @Override
    public int compareTo(ChronoLocalDateTime<?> other) {
        LocalDateTime dateTime = (LocalDateTime) other;
        int cmp = date.compareTo(dateTime.date);
        return cmp != 0 ? cmp : time.compareTo(dateTime.time);
    }

    public boolean isAfter(ChronoLocalDateTime<?> other) {
        return compareTo(other) > 0;
    }

    public boolean isBefore(ChronoLocalDateTime<?> other) {
        return compareTo(other) < 0;
    }

    public boolean isEqual(ChronoLocalDateTime<?> other) {
        return compareTo(other) == 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LocalDateTime other && date.equals(other.date)
                && time.equals(other.time);
    }

    @Override
    public int hashCode() {
        return date.hashCode() ^ time.hashCode();
    }

    @Override
    public String toString() {
        return date.toString() + 'T' + time.toString();
    }
}
