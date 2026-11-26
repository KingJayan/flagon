package java.time;

import java.time.temporal.ChronoField;

public final class Year implements Comparable<Year> {

    public static final int MIN_VALUE = -999_999_999;
    public static final int MAX_VALUE = 999_999_999;

    private final int year;

    private Year(int year) {
        this.year = year;
    }

    public static Year now() {
        return of(LocalDate.now().getYear());
    }

    public static Year now(ZoneId zone) {
        return of(LocalDate.now(zone).getYear());
    }

    public static Year now(Clock clock) {
        return of(LocalDate.now(clock).getYear());
    }

    public static Year of(int isoYear) {
        ChronoField.YEAR.checkValidValue(isoYear);
        return new Year(isoYear);
    }

    public static boolean isLeap(long year) {
        return ((year & 3) == 0) && ((year % 100) != 0 || (year % 400) == 0);
    }

    public int getValue() {
        return year;
    }

    public boolean isLeap() {
        return isLeap(year);
    }

    public int length() {
        return isLeap() ? 366 : 365;
    }

    public LocalDate atDay(int dayOfYear) {
        return LocalDate.ofYearDay(year, dayOfYear);
    }

    public YearMonth atMonth(int month) {
        return YearMonth.of(year, month);
    }

    public YearMonth atMonth(Month month) {
        return YearMonth.of(year, month);
    }

    public Year plusYears(long yearsToAdd) {
        return yearsToAdd == 0 ? this : of(Math.toIntExact(year + yearsToAdd));
    }

    public Year minusYears(long yearsToSubtract) {
        return plusYears(-yearsToSubtract);
    }

    public boolean isAfter(Year other) {
        return year > other.year;
    }

    public boolean isBefore(Year other) {
        return year < other.year;
    }

    @Override
    public int compareTo(Year other) {
        return year - other.year;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Year other && year == other.year;
    }

    @Override
    public int hashCode() {
        return year;
    }

    @Override
    public String toString() {
        return Integer.toString(year);
    }
}
