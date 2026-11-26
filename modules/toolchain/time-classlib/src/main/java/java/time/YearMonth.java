package java.time;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

public final class YearMonth implements Comparable<YearMonth> {

    private final int year;
    private final int month;

    private YearMonth(int year, int month) {
        this.year = year;
        this.month = month;
    }

    public static YearMonth now() {
        LocalDate now = LocalDate.now();
        return of(now.getYear(), now.getMonthValue());
    }

    public static YearMonth now(ZoneId zone) {
        LocalDate now = LocalDate.now(zone);
        return of(now.getYear(), now.getMonthValue());
    }

    public static YearMonth now(Clock clock) {
        LocalDate now = LocalDate.now(clock);
        return of(now.getYear(), now.getMonthValue());
    }

    public static YearMonth of(int year, Month month) {
        return of(year, month.getValue());
    }

    public static YearMonth of(int year, int month) {
        ChronoField.YEAR.checkValidValue(year);
        ChronoField.MONTH_OF_YEAR.checkValidValue(month);
        return new YearMonth(year, month);
    }

    public static YearMonth parse(CharSequence text) {
        try {
            LocalDate date = LocalDate.parse(text + "-01");
            return of(date.getYear(), date.getMonthValue());
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException("Text '" + text + "' could not be parsed", text, 0, e);
        }
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

    public boolean isLeapYear() {
        return Year.isLeap(year);
    }

    public boolean isValidDay(int dayOfMonth) {
        return dayOfMonth >= 1 && dayOfMonth <= lengthOfMonth();
    }

    public int lengthOfMonth() {
        return getMonth().length(isLeapYear());
    }

    public int lengthOfYear() {
        return isLeapYear() ? 366 : 365;
    }

    public YearMonth withYear(int year) {
        return of(year, month);
    }

    public YearMonth withMonth(int month) {
        return of(year, month);
    }

    public YearMonth plusYears(long yearsToAdd) {
        return yearsToAdd == 0 ? this
                : of(ChronoField.YEAR.checkValidIntValue(year + yearsToAdd), month);
    }

    public YearMonth plusMonths(long monthsToAdd) {
        if (monthsToAdd == 0) {
            return this;
        }
        long calcMonths = year * 12L + (month - 1) + monthsToAdd;
        int newYear = ChronoField.YEAR.checkValidIntValue(Math.floorDiv(calcMonths, 12));
        return of(newYear, Math.floorMod(calcMonths, 12) + 1);
    }

    public YearMonth minusYears(long yearsToSubtract) {
        return plusYears(-yearsToSubtract);
    }

    public YearMonth minusMonths(long monthsToSubtract) {
        return plusMonths(-monthsToSubtract);
    }

    public LocalDate atDay(int dayOfMonth) {
        return LocalDate.of(year, month, dayOfMonth);
    }

    public LocalDate atEndOfMonth() {
        return LocalDate.of(year, month, lengthOfMonth());
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(atDay(1));
    }

    @Override
    public int compareTo(YearMonth other) {
        int cmp = year - other.year;
        return cmp == 0 ? month - other.month : cmp;
    }

    public boolean isAfter(YearMonth other) {
        return compareTo(other) > 0;
    }

    public boolean isBefore(YearMonth other) {
        return compareTo(other) < 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof YearMonth other && year == other.year && month == other.month;
    }

    @Override
    public int hashCode() {
        return year ^ (month << 27);
    }

    @Override
    public String toString() {
        String date = LocalDate.of(year, month, 1).toString();
        return date.substring(0, date.length() - 3);
    }
}
