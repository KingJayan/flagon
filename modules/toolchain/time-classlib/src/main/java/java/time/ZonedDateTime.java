package java.time;

import java.time.chrono.ChronoZonedDateTime;
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
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;

public final class ZonedDateTime implements Temporal, ChronoZonedDateTime<LocalDate> {

    private final LocalDateTime dateTime;
    private final ZoneOffset offset;
    private final ZoneId zone;

    private ZonedDateTime(LocalDateTime dateTime, ZoneOffset offset, ZoneId zone) {
        this.dateTime = dateTime;
        this.offset = offset;
        this.zone = zone;
    }

    public static ZonedDateTime now() {
        return now(Clock.systemDefaultZone());
    }

    public static ZonedDateTime now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    public static ZonedDateTime now(Clock clock) {
        return ofInstant(clock.instant(), clock.getZone());
    }

    public static ZonedDateTime of(LocalDate date, LocalTime time, ZoneId zone) {
        return of(LocalDateTime.of(date, time), zone);
    }

    public static ZonedDateTime of(LocalDateTime localDateTime, ZoneId zone) {
        return ofLocal(localDateTime, zone, null);
    }

    public static ZonedDateTime of(int year, int month, int dayOfMonth, int hour, int minute,
            int second, int nanoOfSecond, ZoneId zone) {
        return ofLocal(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second,
                nanoOfSecond),
                zone, null);
    }

    public static ZonedDateTime ofLocal(LocalDateTime localDateTime, ZoneId zone,
            ZoneOffset preferredOffset) {
        if (zone instanceof ZoneOffset fixed) {
            return new ZonedDateTime(localDateTime, fixed, zone);
        }
        ZoneRules rules = zone.getRules();
        ZoneOffsetTransition transition = rules.getTransition(localDateTime);
        ZoneOffset offset;
        if (transition == null) {
            offset = rules.getOffset(localDateTime);
        } else if (transition.isGap()) {
            localDateTime = localDateTime.plusSeconds(transition.getDuration().getSeconds());
            offset = transition.getOffsetAfter();
        } else {
            offset = preferredOffset != null && transition.isValidOffset(preferredOffset)
                    ? preferredOffset : transition.getOffsetBefore();
        }
        return new ZonedDateTime(localDateTime, offset, zone);
    }

    public static ZonedDateTime ofInstant(Instant instant, ZoneId zone) {
        return create(instant.getEpochSecond(), instant.getNano(), zone);
    }

    public static ZonedDateTime ofInstant(LocalDateTime localDateTime, ZoneOffset offset,
            ZoneId zone) {
        return create(localDateTime.toEpochSecond(offset), localDateTime.getNano(), zone);
    }

    private static ZonedDateTime create(long epochSecond, int nanoOfSecond, ZoneId zone) {
        ZoneOffset offset = zone.getRules().getOffset(Instant.ofEpochSecond(epochSecond,
                nanoOfSecond));
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(epochSecond, nanoOfSecond, offset);
        return new ZonedDateTime(ldt, offset, zone);
    }

    public static ZonedDateTime ofStrict(LocalDateTime localDateTime, ZoneOffset offset,
            ZoneId zone) {
        if (!zone.getRules().isValidOffset(localDateTime, offset)) {
            throw new DateTimeException("ZoneOffset '" + offset
                    + "' is not valid for LocalDateTime '"
                    + localDateTime + "' in zone '" + zone + "'");
        }
        return new ZonedDateTime(localDateTime, offset, zone);
    }

    public static ZonedDateTime from(TemporalAccessor temporal) {
        if (temporal instanceof ZonedDateTime zoned) {
            return zoned;
        }
        try {
            ZoneId zone = ZoneId.from(temporal);
            if (temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
                return create(temporal.getLong(ChronoField.INSTANT_SECONDS),
                        temporal.get(ChronoField.NANO_OF_SECOND), zone);
            }
            return of(LocalDateTime.from(temporal), zone);
        } catch (DateTimeException e) {
            throw new DateTimeException("Unable to obtain ZonedDateTime from TemporalAccessor: "
                    + temporal + " of type " + temporal.getClass().getName(), e);
        }
    }

    public static ZonedDateTime parse(CharSequence text) {
        return parse(text, DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    public static ZonedDateTime parse(CharSequence text, DateTimeFormatter formatter) {
        return formatter.parse(text, ZonedDateTime::from);
    }

    private ZonedDateTime resolveLocal(LocalDateTime newDateTime) {
        return ofLocal(newDateTime, zone, offset);
    }

    private ZonedDateTime resolveInstant(LocalDateTime newDateTime) {
        return ofInstant(newDateTime, offset, zone);
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
        return ChronoZonedDateTime.super.get(field);
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

    public ZoneId getZone() {
        return zone;
    }

    public ZonedDateTime withEarlierOffsetAtOverlap() {
        ZoneOffsetTransition transition = zone.getRules().getTransition(dateTime);
        return transition != null && transition.isOverlap()
                ? new ZonedDateTime(dateTime, transition.getOffsetBefore(), zone) : this;
    }

    public ZonedDateTime withLaterOffsetAtOverlap() {
        ZoneOffsetTransition transition = zone.getRules().getTransition(dateTime);
        return transition != null && transition.isOverlap()
                ? new ZonedDateTime(dateTime, transition.getOffsetAfter(), zone) : this;
    }

    public ZonedDateTime withZoneSameLocal(ZoneId zone) {
        return this.zone.equals(zone) ? this : ofLocal(dateTime, zone, offset);
    }

    public ZonedDateTime withZoneSameInstant(ZoneId zone) {
        return this.zone.equals(zone) ? this
                : create(dateTime.toEpochSecond(offset), dateTime.getNano(), zone);
    }

    public ZonedDateTime withFixedOffsetZone() {
        return zone.equals(offset) ? this : new ZonedDateTime(dateTime, offset, offset);
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
    public ZonedDateTime with(TemporalAdjuster adjuster) {
        if (adjuster instanceof LocalDate date) {
            return resolveLocal(LocalDateTime.of(date, dateTime.toLocalTime()));
        }
        if (adjuster instanceof LocalTime time) {
            return resolveLocal(LocalDateTime.of(dateTime.toLocalDate(), time));
        }
        if (adjuster instanceof LocalDateTime local) {
            return resolveLocal(local);
        }
        if (adjuster instanceof Instant instant) {
            return create(instant.getEpochSecond(), instant.getNano(), zone);
        }
        if (adjuster instanceof ZoneOffset newOffset) {
            return zone.getRules().isValidOffset(dateTime, newOffset) && !newOffset.equals(offset)
                    ? new ZonedDateTime(dateTime, newOffset, zone) : this;
        }
        return (ZonedDateTime) adjuster.adjustInto(this);
    }

    @Override
    public ZonedDateTime with(TemporalField field, long newValue) {
        if (field instanceof ChronoField chrono) {
            switch (chrono) {
                case INSTANT_SECONDS:
                    return create(newValue, getNano(), zone);
                case OFFSET_SECONDS:
                    return with(ZoneOffset.ofTotalSeconds(chrono.checkValidIntValue(newValue)));
                default:
                    return resolveLocal(dateTime.with(field, newValue));
            }
        }
        return field.adjustInto(this, newValue);
    }

    public ZonedDateTime withYear(int year) {
        return resolveLocal(dateTime.withYear(year));
    }

    public ZonedDateTime withMonth(int month) {
        return resolveLocal(dateTime.withMonth(month));
    }

    public ZonedDateTime withDayOfMonth(int dayOfMonth) {
        return resolveLocal(dateTime.withDayOfMonth(dayOfMonth));
    }

    public ZonedDateTime withDayOfYear(int dayOfYear) {
        return resolveLocal(dateTime.withDayOfYear(dayOfYear));
    }

    public ZonedDateTime withHour(int hour) {
        return resolveLocal(dateTime.withHour(hour));
    }

    public ZonedDateTime withMinute(int minute) {
        return resolveLocal(dateTime.withMinute(minute));
    }

    public ZonedDateTime withSecond(int second) {
        return resolveLocal(dateTime.withSecond(second));
    }

    public ZonedDateTime withNano(int nanoOfSecond) {
        return resolveLocal(dateTime.withNano(nanoOfSecond));
    }

    public ZonedDateTime truncatedTo(TemporalUnit unit) {
        return resolveLocal(dateTime.truncatedTo(unit));
    }

    @Override
    public ZonedDateTime plus(TemporalAmount amountToAdd) {
        if (amountToAdd instanceof Period period) {
            return resolveLocal(dateTime.plus(period));
        }
        return (ZonedDateTime) amountToAdd.addTo(this);
    }

    @Override
    public ZonedDateTime plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isDateBased() ? resolveLocal(dateTime.plus(amountToAdd, unit))
                    : resolveInstant(dateTime.plus(amountToAdd, unit));
        }
        return unit.addTo(this, amountToAdd);
    }

    public ZonedDateTime plusYears(long years) {
        return resolveLocal(dateTime.plusYears(years));
    }

    public ZonedDateTime plusMonths(long months) {
        return resolveLocal(dateTime.plusMonths(months));
    }

    public ZonedDateTime plusWeeks(long weeks) {
        return resolveLocal(dateTime.plusWeeks(weeks));
    }

    public ZonedDateTime plusDays(long days) {
        return resolveLocal(dateTime.plusDays(days));
    }

    public ZonedDateTime plusHours(long hours) {
        return resolveInstant(dateTime.plusHours(hours));
    }

    public ZonedDateTime plusMinutes(long minutes) {
        return resolveInstant(dateTime.plusMinutes(minutes));
    }

    public ZonedDateTime plusSeconds(long seconds) {
        return resolveInstant(dateTime.plusSeconds(seconds));
    }

    public ZonedDateTime plusNanos(long nanos) {
        return resolveInstant(dateTime.plusNanos(nanos));
    }

    @Override
    public ZonedDateTime minus(TemporalAmount amountToSubtract) {
        if (amountToSubtract instanceof Period period) {
            return resolveLocal(dateTime.minus(period));
        }
        return (ZonedDateTime) amountToSubtract.subtractFrom(this);
    }

    @Override
    public ZonedDateTime minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE
                ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit);
    }

    public ZonedDateTime minusYears(long years) {
        return plusYears(-years);
    }

    public ZonedDateTime minusMonths(long months) {
        return plusMonths(-months);
    }

    public ZonedDateTime minusWeeks(long weeks) {
        return plusWeeks(-weeks);
    }

    public ZonedDateTime minusDays(long days) {
        return plusDays(-days);
    }

    public ZonedDateTime minusHours(long hours) {
        return plusHours(-hours);
    }

    public ZonedDateTime minusMinutes(long minutes) {
        return plusMinutes(-minutes);
    }

    public ZonedDateTime minusSeconds(long seconds) {
        return plusSeconds(-seconds);
    }

    public ZonedDateTime minusNanos(long nanos) {
        return plusNanos(-nanos);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.localDate()) {
            return (R) toLocalDate();
        }
        if (query == TemporalQueries.localTime()) {
            return (R) toLocalTime();
        }
        if (query == TemporalQueries.zone() || query == TemporalQueries.zoneId()) {
            return (R) zone;
        }
        if (query == TemporalQueries.offset()) {
            return (R) offset;
        }
        if (query == TemporalQueries.precision()) {
            return (R) ChronoUnit.NANOS;
        }
        return ChronoZonedDateTime.super.query(query);
    }

    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        ZonedDateTime end = ZonedDateTime.from(endExclusive);
        if (unit instanceof ChronoUnit) {
            ZonedDateTime start = withZoneSameInstant(end.zone);
            return unit.isDateBased() ? start.dateTime.until(end.dateTime, unit)
                    : start.toOffsetDateTime().until(end.toOffsetDateTime(), unit);
        }
        return unit.between(this, end);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    public OffsetDateTime toOffsetDateTime() {
        return OffsetDateTime.of(dateTime, offset);
    }

    public long toEpochSecond() {
        return dateTime.toEpochSecond(offset);
    }

    @Override
    public Instant toInstant() {
        return Instant.ofEpochSecond(toEpochSecond(), getNano());
    }

    @Override
    public int compareTo(ChronoZonedDateTime<?> other) {
        ZonedDateTime zoned = (ZonedDateTime) other;
        int cmp = Long.compare(toEpochSecond(), zoned.toEpochSecond());
        if (cmp == 0) {
            cmp = getNano() - zoned.getNano();
            if (cmp == 0) {
                cmp = dateTime.compareTo(zoned.dateTime);
                if (cmp == 0) {
                    cmp = zone.getId().compareTo(zoned.zone.getId());
                }
            }
        }
        return cmp;
    }

    public boolean isAfter(ChronoZonedDateTime<?> other) {
        return toInstant().isAfter(other.toInstant());
    }

    public boolean isBefore(ChronoZonedDateTime<?> other) {
        return toInstant().isBefore(other.toInstant());
    }

    public boolean isEqual(ChronoZonedDateTime<?> other) {
        return toInstant().equals(other.toInstant());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ZonedDateTime other && dateTime.equals(other.dateTime)
                && offset.equals(other.offset) && zone.equals(other.zone);
    }

    @Override
    public int hashCode() {
        return dateTime.hashCode() ^ offset.hashCode() ^ Integer.rotateLeft(zone.hashCode(), 3);
    }

    @Override
    public String toString() {
        String str = dateTime.toString() + offset.toString();
        if (offset != zone) {
            str += '[' + zone.toString() + ']';
        }
        return str;
    }
}
