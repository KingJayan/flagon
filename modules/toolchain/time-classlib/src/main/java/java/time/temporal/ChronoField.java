package java.time.temporal;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.ERAS;
import static java.time.temporal.ChronoUnit.FOREVER;
import static java.time.temporal.ChronoUnit.HALF_DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.time.temporal.ChronoUnit.YEARS;

public enum ChronoField implements TemporalField {
    NANO_OF_SECOND("NanoOfSecond", NANOS, SECONDS, 0, 999_999_999),
    NANO_OF_DAY("NanoOfDay", NANOS, DAYS, 0, 86400L * 1_000_000_000L - 1),
    MICRO_OF_SECOND("MicroOfSecond", MICROS, SECONDS, 0, 999_999),
    MICRO_OF_DAY("MicroOfDay", MICROS, DAYS, 0, 86400L * 1_000_000L - 1),
    MILLI_OF_SECOND("MilliOfSecond", MILLIS, SECONDS, 0, 999),
    MILLI_OF_DAY("MilliOfDay", MILLIS, DAYS, 0, 86400L * 1000L - 1),
    SECOND_OF_MINUTE("SecondOfMinute", SECONDS, MINUTES, 0, 59),
    SECOND_OF_DAY("SecondOfDay", SECONDS, DAYS, 0, 86400L - 1),
    MINUTE_OF_HOUR("MinuteOfHour", MINUTES, HOURS, 0, 59),
    MINUTE_OF_DAY("MinuteOfDay", MINUTES, DAYS, 0, (24 * 60) - 1),
    HOUR_OF_AMPM("HourOfAmPm", HOURS, HALF_DAYS, 0, 11),
    CLOCK_HOUR_OF_AMPM("ClockHourOfAmPm", HOURS, HALF_DAYS, 1, 12),
    HOUR_OF_DAY("HourOfDay", HOURS, DAYS, 0, 23),
    CLOCK_HOUR_OF_DAY("ClockHourOfDay", HOURS, DAYS, 1, 24),
    AMPM_OF_DAY("AmPmOfDay", HALF_DAYS, DAYS, 0, 1),
    DAY_OF_WEEK("DayOfWeek", DAYS, WEEKS, 1, 7),
    ALIGNED_DAY_OF_WEEK_IN_MONTH("AlignedDayOfWeekInMonth", DAYS, WEEKS, 1, 7),
    ALIGNED_DAY_OF_WEEK_IN_YEAR("AlignedDayOfWeekInYear", DAYS, WEEKS, 1, 7),
    DAY_OF_MONTH("DayOfMonth", DAYS, MONTHS, 1, 31),
    DAY_OF_YEAR("DayOfYear", DAYS, YEARS, 1, 366),
    EPOCH_DAY("EpochDay", DAYS, FOREVER, -365243219162L, 365241780471L),
    ALIGNED_WEEK_OF_MONTH("AlignedWeekOfMonth", WEEKS, MONTHS, 1, 5),
    ALIGNED_WEEK_OF_YEAR("AlignedWeekOfYear", WEEKS, YEARS, 1, 53),
    MONTH_OF_YEAR("MonthOfYear", MONTHS, YEARS, 1, 12),
    PROLEPTIC_MONTH("ProlepticMonth", MONTHS, FOREVER, -999999999L * 12, 999999999L * 12 + 11),
    YEAR_OF_ERA("YearOfEra", YEARS, FOREVER, 1, 1_000_000_000),
    YEAR("Year", YEARS, FOREVER, -999_999_999, 999_999_999),
    ERA("Era", ERAS, FOREVER, 0, 1),
    INSTANT_SECONDS("InstantSeconds", SECONDS, FOREVER, Long.MIN_VALUE, Long.MAX_VALUE),
    OFFSET_SECONDS("OffsetSeconds", SECONDS, FOREVER, -18 * 3600, 18 * 3600);

    private final String name;
    private final TemporalUnit baseUnit;
    private final TemporalUnit rangeUnit;
    private final ValueRange range;

    ChronoField(String name, TemporalUnit baseUnit, TemporalUnit rangeUnit, long min, long max) {
        this.name = name;
        this.baseUnit = baseUnit;
        this.rangeUnit = rangeUnit;
        this.range = ValueRange.of(min, max);
    }

    @Override
    public TemporalUnit getBaseUnit() {
        return baseUnit;
    }

    @Override
    public TemporalUnit getRangeUnit() {
        return rangeUnit;
    }

    @Override
    public ValueRange range() {
        return range;
    }

    @Override
    public boolean isDateBased() {
        return ordinal() >= DAY_OF_WEEK.ordinal() && ordinal() <= ERA.ordinal();
    }

    @Override
    public boolean isTimeBased() {
        return ordinal() < DAY_OF_WEEK.ordinal();
    }

    public long checkValidValue(long value) {
        return range.checkValidValue(value, this);
    }

    public int checkValidIntValue(long value) {
        return range.checkValidIntValue(value, this);
    }

    @Override
    public boolean isSupportedBy(TemporalAccessor temporal) {
        return temporal.isSupported(this);
    }

    @Override
    public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
        return temporal.range(this);
    }

    @Override
    public long getFrom(TemporalAccessor temporal) {
        return temporal.getLong(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Temporal> R adjustInto(R temporal, long newValue) {
        return (R) temporal.with(this, newValue);
    }

    @Override
    public String toString() {
        return name;
    }
}
