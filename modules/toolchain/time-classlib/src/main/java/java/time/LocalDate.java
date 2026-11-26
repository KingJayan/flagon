package java.time;

import java.time.chrono.ChronoLocalDate;
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
import java.time.temporal.ValueRange;

public final class LocalDate implements Temporal, TemporalAdjuster, ChronoLocalDate {

    public static final LocalDate MIN = new LocalDate(Year.MIN_VALUE, 1, 1);
    public static final LocalDate MAX = new LocalDate(Year.MAX_VALUE, 12, 31);
    public static final LocalDate EPOCH = new LocalDate(1970, 1, 1);

    static final long DAYS_0000_TO_1970 = (146097 * 5L) - (30L * 365L + 7L);
    private static final int DAYS_PER_CYCLE = 146097;

    private final int year;
    private final short month;
    private final short day;

    private LocalDate(int year, int month, int day) {
        this.year = year;
        this.month = (short) month;
        this.day = (short) day;
    }

    public static LocalDate now() {
        return now(Clock.systemDefaultZone());
    }

    public static LocalDate now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    public static LocalDate now(Clock clock) {
        return ofInstant(clock.instant(), clock.getZone());
    }

    public static LocalDate of(int year, Month month, int dayOfMonth) {
        return of(year, month.getValue(), dayOfMonth);
    }

    public static LocalDate of(int year, int month, int dayOfMonth) {
        ChronoField.YEAR.checkValidValue(year);
        ChronoField.MONTH_OF_YEAR.checkValidValue(month);
        ChronoField.DAY_OF_MONTH.checkValidValue(dayOfMonth);
        if (dayOfMonth > 28 && dayOfMonth > Month.of(month).length(Year.isLeap(year))) {
            if (dayOfMonth == 29) {
                throw new DateTimeException("Invalid date 'February 29' as '" + year
                        + "' is not a leap year");
            }
            throw new DateTimeException("Invalid date '" + Month.of(month).name() + " "
                    + dayOfMonth + "'");
        }
        return new LocalDate(year, month, dayOfMonth);
    }

    public static LocalDate ofYearDay(int year, int dayOfYear) {
        ChronoField.YEAR.checkValidValue(year);
        ChronoField.DAY_OF_YEAR.checkValidValue(dayOfYear);
        boolean leap = Year.isLeap(year);
        if (dayOfYear == 366 && !leap) {
            throw new DateTimeException("Invalid date 'DayOfYear 366' as '" + year
                    + "' is not a leap year");
        }
        Month moy = Month.of((dayOfYear - 1) / 31 + 1);
        int monthEnd = moy.firstDayOfYear(leap) + moy.length(leap) - 1;
        if (dayOfYear > monthEnd) {
            moy = moy.plus(1);
        }
        int dom = dayOfYear - moy.firstDayOfYear(leap) + 1;
        return new LocalDate(year, moy.getValue(), dom);
    }

    public static LocalDate ofInstant(Instant instant, ZoneId zone) {
        ZoneOffset offset = zone.getRules().getOffset(instant);
        long localSecond = instant.getEpochSecond() + offset.getTotalSeconds();
        return ofEpochDay(Math.floorDiv(localSecond, 86400L));
    }

    public static LocalDate ofEpochDay(long epochDay) {
        ChronoField.EPOCH_DAY.checkValidValue(epochDay);
        long zeroDay = epochDay + DAYS_0000_TO_1970;
        zeroDay -= 60;
        long adjust = 0;
        if (zeroDay < 0) {
            long adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1;
            adjust = adjustCycles * 400;
            zeroDay += -adjustCycles * DAYS_PER_CYCLE;
        }
        long yearEst = (400 * zeroDay + 591) / DAYS_PER_CYCLE;
        long doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        if (doyEst < 0) {
            yearEst--;
            doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        }
        yearEst += adjust;
        int marchDoy0 = (int) doyEst;
        int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
        int month = (marchMonth0 + 2) % 12 + 1;
        int dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
        yearEst += marchMonth0 / 10;
        return new LocalDate((int) yearEst, month, dom);
    }

    public static LocalDate from(TemporalAccessor temporal) {
        LocalDate date = temporal.query(TemporalQueries.localDate());
        if (date == null) {
            throw new DateTimeException("Unable to obtain LocalDate from TemporalAccessor: "
                    + temporal + " of type " + temporal.getClass().getName());
        }
        return date;
    }

    public static LocalDate parse(CharSequence text) {
        return parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static LocalDate parse(CharSequence text, DateTimeFormatter formatter) {
        return formatter.parse(text, LocalDate::from);
    }

    private static LocalDate resolvePreviousValid(int year, int month, int day) {
        return new LocalDate(year, month, Math.min(day, Month.of(month).length(Year.isLeap(year))));
    }

    @Override
    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField chrono) {
            return chrono.isDateBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isDateBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public ValueRange range(TemporalField field) {
        if (field instanceof ChronoField chrono) {
            if (!chrono.isDateBased()) {
                throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            }
            switch (chrono) {
                case DAY_OF_MONTH:
                    return ValueRange.of(1, lengthOfMonth());
                case DAY_OF_YEAR:
                    return ValueRange.of(1, lengthOfYear());
                case ALIGNED_WEEK_OF_MONTH:
                    return ValueRange.of(1, getMonth() == Month.FEBRUARY && !isLeapYear() ? 4 : 5);
                case YEAR_OF_ERA:
                    return year <= 0 ? ValueRange.of(1, Year.MAX_VALUE + 1)
                            : ValueRange.of(1, Year.MAX_VALUE);
                default:
                    return field.range();
            }
        }
        return field.rangeRefinedBy(this);
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
            case DAY_OF_WEEK:
                return getDayOfWeek().getValue();
            case ALIGNED_DAY_OF_WEEK_IN_MONTH:
                return ((day - 1) % 7) + 1;
            case ALIGNED_DAY_OF_WEEK_IN_YEAR:
                return ((getDayOfYear() - 1) % 7) + 1;
            case DAY_OF_MONTH:
                return day;
            case DAY_OF_YEAR:
                return getDayOfYear();
            case EPOCH_DAY:
                return toEpochDay();
            case ALIGNED_WEEK_OF_MONTH:
                return ((day - 1) / 7) + 1;
            case ALIGNED_WEEK_OF_YEAR:
                return ((getDayOfYear() - 1) / 7) + 1;
            case MONTH_OF_YEAR:
                return month;
            case PROLEPTIC_MONTH:
                return getProlepticMonth();
            case YEAR_OF_ERA:
                return year >= 1 ? year : 1 - year;
            case YEAR:
                return year;
            case ERA:
                return year >= 1 ? 1 : 0;
            default:
                throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
    }

    private long getProlepticMonth() {
        return year * 12L + month - 1;
    }

    public int getYear() {
        return year;
    }

    public int getMonthValue() {
        return month;
    }

    public Month getMonth() {
        return Month.of(month);
    }

    public int getDayOfMonth() {
        return day;
    }

    public int getDayOfYear() {
        return getMonth().firstDayOfYear(isLeapYear()) + day - 1;
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.of((int) Math.floorMod(toEpochDay() + 3, 7L) + 1);
    }

    public boolean isLeapYear() {
        return Year.isLeap(year);
    }

    public int lengthOfMonth() {
        return getMonth().length(isLeapYear());
    }

    public int lengthOfYear() {
        return isLeapYear() ? 366 : 365;
    }

    @Override
    public LocalDate with(TemporalAdjuster adjuster) {
        if (adjuster instanceof LocalDate date) {
            return date;
        }
        return (LocalDate) adjuster.adjustInto(this);
    }

    @Override
    public LocalDate with(TemporalField field, long newValue) {
        if (!(field instanceof ChronoField chrono)) {
            return field.adjustInto(this, newValue);
        }
        chrono.checkValidValue(newValue);
        switch (chrono) {
            case DAY_OF_WEEK:
                return plusDays(newValue - getDayOfWeek().getValue());
            case ALIGNED_DAY_OF_WEEK_IN_MONTH:
                return plusDays(newValue - getLong(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH));
            case ALIGNED_DAY_OF_WEEK_IN_YEAR:
                return plusDays(newValue - getLong(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR));
            case DAY_OF_MONTH:
                return withDayOfMonth((int) newValue);
            case DAY_OF_YEAR:
                return withDayOfYear((int) newValue);
            case EPOCH_DAY:
                return ofEpochDay(newValue);
            case ALIGNED_WEEK_OF_MONTH:
                return plusWeeks(newValue - getLong(ChronoField.ALIGNED_WEEK_OF_MONTH));
            case ALIGNED_WEEK_OF_YEAR:
                return plusWeeks(newValue - getLong(ChronoField.ALIGNED_WEEK_OF_YEAR));
            case MONTH_OF_YEAR:
                return withMonth((int) newValue);
            case PROLEPTIC_MONTH:
                return plusMonths(newValue - getProlepticMonth());
            case YEAR_OF_ERA:
                return withYear((int) (year >= 1 ? newValue : 1 - newValue));
            case YEAR:
                return withYear((int) newValue);
            case ERA:
                return getLong(ChronoField.ERA) == newValue ? this : withYear(1 - year);
            default:
                throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
    }

    public LocalDate withYear(int year) {
        if (this.year == year) {
            return this;
        }
        ChronoField.YEAR.checkValidValue(year);
        return resolvePreviousValid(year, month, day);
    }

    public LocalDate withMonth(int month) {
        if (this.month == month) {
            return this;
        }
        ChronoField.MONTH_OF_YEAR.checkValidValue(month);
        return resolvePreviousValid(year, month, day);
    }

    public LocalDate withDayOfMonth(int dayOfMonth) {
        return day == dayOfMonth ? this : of(year, month, dayOfMonth);
    }

    public LocalDate withDayOfYear(int dayOfYear) {
        return getDayOfYear() == dayOfYear ? this : ofYearDay(year, dayOfYear);
    }

    @Override
    public LocalDate plus(TemporalAmount amountToAdd) {
        if (amountToAdd instanceof Period period) {
            return plusMonths(period.toTotalMonths()).plusDays(period.getDays());
        }
        return (LocalDate) amountToAdd.addTo(this);
    }

    @Override
    public LocalDate plus(long amountToAdd, TemporalUnit unit) {
        if (!(unit instanceof ChronoUnit chrono)) {
            return unit.addTo(this, amountToAdd);
        }
        switch (chrono) {
            case DAYS:
                return plusDays(amountToAdd);
            case WEEKS:
                return plusWeeks(amountToAdd);
            case MONTHS:
                return plusMonths(amountToAdd);
            case YEARS:
                return plusYears(amountToAdd);
            case DECADES:
                return plusYears(Math.multiplyExact(amountToAdd, 10));
            case CENTURIES:
                return plusYears(Math.multiplyExact(amountToAdd, 100));
            case MILLENNIA:
                return plusYears(Math.multiplyExact(amountToAdd, 1000));
            case ERAS:
                return with(ChronoField.ERA, Math.addExact(getLong(ChronoField.ERA), amountToAdd));
            default:
                throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
    }

    public LocalDate plusYears(long yearsToAdd) {
        if (yearsToAdd == 0) {
            return this;
        }
        int newYear = ChronoField.YEAR.checkValidIntValue(year + yearsToAdd);
        return resolvePreviousValid(newYear, month, day);
    }

    public LocalDate plusMonths(long monthsToAdd) {
        if (monthsToAdd == 0) {
            return this;
        }
        long monthCount = year * 12L + (month - 1);
        long calcMonths = monthCount + monthsToAdd;
        int newYear = ChronoField.YEAR.checkValidIntValue(Math.floorDiv(calcMonths, 12));
        int newMonth = Math.floorMod(calcMonths, 12) + 1;
        return resolvePreviousValid(newYear, newMonth, day);
    }

    public LocalDate plusWeeks(long weeksToAdd) {
        return plusDays(Math.multiplyExact(weeksToAdd, 7));
    }

    public LocalDate plusDays(long daysToAdd) {
        if (daysToAdd == 0) {
            return this;
        }
        long dom = day + daysToAdd;
        if (dom > 0 && dom <= 28) {
            return new LocalDate(year, month, (int) dom);
        }
        return ofEpochDay(Math.addExact(toEpochDay(), daysToAdd));
    }

    @Override
    public LocalDate minus(TemporalAmount amountToSubtract) {
        if (amountToSubtract instanceof Period period) {
            return minusMonths(period.toTotalMonths()).minusDays(period.getDays());
        }
        return (LocalDate) amountToSubtract.subtractFrom(this);
    }

    @Override
    public LocalDate minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE
                ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit);
    }

    public LocalDate minusYears(long yearsToSubtract) {
        return yearsToSubtract == Long.MIN_VALUE
                ? plusYears(Long.MAX_VALUE).plusYears(1) : plusYears(-yearsToSubtract);
    }

    public LocalDate minusMonths(long monthsToSubtract) {
        return monthsToSubtract == Long.MIN_VALUE
                ? plusMonths(Long.MAX_VALUE).plusMonths(1) : plusMonths(-monthsToSubtract);
    }

    public LocalDate minusWeeks(long weeksToSubtract) {
        return weeksToSubtract == Long.MIN_VALUE
                ? plusWeeks(Long.MAX_VALUE).plusWeeks(1) : plusWeeks(-weeksToSubtract);
    }

    public LocalDate minusDays(long daysToSubtract) {
        return daysToSubtract == Long.MIN_VALUE
                ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.localDate()) {
            return (R) this;
        }
        if (query == TemporalQueries.precision()) {
            return (R) ChronoUnit.DAYS;
        }
        return ChronoLocalDate.super.query(query);
    }

    @Override
    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.EPOCH_DAY, toEpochDay());
    }

    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        LocalDate end = LocalDate.from(endExclusive);
        if (!(unit instanceof ChronoUnit chrono)) {
            return unit.between(this, end);
        }
        switch (chrono) {
            case DAYS:
                return end.toEpochDay() - toEpochDay();
            case WEEKS:
                return (end.toEpochDay() - toEpochDay()) / 7;
            case MONTHS:
                return monthsUntil(end);
            case YEARS:
                return monthsUntil(end) / 12;
            case DECADES:
                return monthsUntil(end) / 120;
            case CENTURIES:
                return monthsUntil(end) / 1200;
            case MILLENNIA:
                return monthsUntil(end) / 12000;
            case ERAS:
                return end.getLong(ChronoField.ERA) - getLong(ChronoField.ERA);
            default:
                throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
    }

    private long monthsUntil(LocalDate end) {
        long packed1 = getProlepticMonth() * 32L + day;
        long packed2 = end.getProlepticMonth() * 32L + end.day;
        return (packed2 - packed1) / 32;
    }

    public Period until(LocalDate endDateExclusive) {
        LocalDate end = endDateExclusive;
        long totalMonths = end.getProlepticMonth() - getProlepticMonth();
        int days = end.day - day;
        if (totalMonths > 0 && days < 0) {
            totalMonths--;
            LocalDate calcDate = plusMonths(totalMonths);
            days = (int) (end.toEpochDay() - calcDate.toEpochDay());
        } else if (totalMonths < 0 && days > 0) {
            totalMonths++;
            days -= end.lengthOfMonth();
        }
        long years = totalMonths / 12;
        int months = (int) (totalMonths % 12);
        return Period.of(Math.toIntExact(years), months, days);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    public LocalDateTime atTime(LocalTime time) {
        return LocalDateTime.of(this, time);
    }

    public LocalDateTime atTime(int hour, int minute) {
        return atTime(LocalTime.of(hour, minute));
    }

    public LocalDateTime atTime(int hour, int minute, int second) {
        return atTime(LocalTime.of(hour, minute, second));
    }

    public LocalDateTime atTime(int hour, int minute, int second, int nanoOfSecond) {
        return atTime(LocalTime.of(hour, minute, second, nanoOfSecond));
    }

    public LocalDateTime atStartOfDay() {
        return LocalDateTime.of(this, LocalTime.MIDNIGHT);
    }

    public ZonedDateTime atStartOfDay(ZoneId zone) {
        return ZonedDateTime.of(atStartOfDay(), zone);
    }

    @Override
    public long toEpochDay() {
        long y = year;
        long m = month;
        long total = 0;
        total += 365 * y;
        if (y >= 0) {
            total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
        } else {
            total -= y / -4 - y / -100 + y / -400;
        }
        total += ((367 * m - 362) / 12);
        total += day - 1;
        if (m > 2) {
            total--;
            if (!isLeapYear()) {
                total--;
            }
        }
        return total - DAYS_0000_TO_1970;
    }

    public long toEpochSecond(LocalTime time, ZoneOffset offset) {
        return toEpochDay() * 86400 + time.toSecondOfDay() - offset.getTotalSeconds();
    }

    @Override
    public int compareTo(ChronoLocalDate other) {
        LocalDate date = (LocalDate) other;
        int cmp = year - date.year;
        if (cmp == 0) {
            cmp = month - date.month;
            if (cmp == 0) {
                cmp = day - date.day;
            }
        }
        return cmp;
    }

    public boolean isAfter(ChronoLocalDate other) {
        return compareTo(other) > 0;
    }

    public boolean isBefore(ChronoLocalDate other) {
        return compareTo(other) < 0;
    }

    public boolean isEqual(ChronoLocalDate other) {
        return compareTo(other) == 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LocalDate other && compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        return (year & 0xFFFFF800) ^ ((year << 11) + (month << 6) + (day));
    }

    @Override
    public String toString() {
        int absYear = Math.abs(year);
        StringBuilder buf = new StringBuilder(10);
        if (absYear < 1000) {
            if (year < 0) {
                buf.append(year - 10000).deleteCharAt(1);
            } else {
                buf.append(year + 10000).deleteCharAt(0);
            }
        } else {
            if (year > 9999) {
                buf.append('+');
            }
            buf.append(year);
        }
        return buf.append(month < 10 ? "-0" : "-").append(month)
                .append(day < 10 ? "-0" : "-").append(day).toString();
    }
}
