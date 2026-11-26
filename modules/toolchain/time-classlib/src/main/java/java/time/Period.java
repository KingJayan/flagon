package java.time;

import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Period implements TemporalAmount {

    public static final Period ZERO = new Period(0, 0, 0);

    private static final Pattern PATTERN = Pattern.compile(
            "([-+]?)P(?:([-+]?[0-9]+)Y)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)W)?(?:([-+]?[0-9]+)D)?",
            Pattern.CASE_INSENSITIVE);

    private final int years;
    private final int months;
    private final int days;

    private Period(int years, int months, int days) {
        this.years = years;
        this.months = months;
        this.days = days;
    }

    public static Period of(int years, int months, int days) {
        return (years | months | days) == 0 ? ZERO : new Period(years, months, days);
    }

    public static Period ofYears(int years) {
        return of(years, 0, 0);
    }

    public static Period ofMonths(int months) {
        return of(0, months, 0);
    }

    public static Period ofWeeks(int weeks) {
        return of(0, 0, Math.multiplyExact(weeks, 7));
    }

    public static Period ofDays(int days) {
        return of(0, 0, days);
    }

    public static Period from(TemporalAmount amount) {
        if (amount instanceof Period period) {
            return period;
        }
        int years = 0;
        int months = 0;
        int days = 0;
        for (TemporalUnit unit : amount.getUnits()) {
            long value = amount.get(unit);
            if (unit == ChronoUnit.YEARS) {
                years = Math.toIntExact(value);
            } else if (unit == ChronoUnit.MONTHS) {
                months = Math.toIntExact(value);
            } else if (unit == ChronoUnit.DAYS) {
                days = Math.toIntExact(value);
            } else {
                throw new DateTimeException("Unit must be Years, Months or Days, but was " + unit);
            }
        }
        return of(years, months, days);
    }

    public static Period between(LocalDate startDateInclusive, LocalDate endDateExclusive) {
        return startDateInclusive.until(endDateExclusive);
    }

    public static Period parse(CharSequence text) {
        Matcher matcher = PATTERN.matcher(text);
        if (matcher.matches()) {
            int negate = "-".equals(matcher.group(1)) ? -1 : 1;
            if (matcher.group(2) != null || matcher.group(3) != null || matcher.group(4) != null
                    || matcher.group(5) != null) {
                try {
                    int years = number(matcher.group(2), negate);
                    int months = number(matcher.group(3), negate);
                    int weeks = number(matcher.group(4), negate);
                    int days = number(matcher.group(5), negate);
                    return of(years, months, Math.addExact(days, Math.multiplyExact(weeks, 7)));
                } catch (NumberFormatException | ArithmeticException e) {
                    throw new DateTimeParseException("Text cannot be parsed to a Period", text, 0,
                            e);
                }
            }
        }
        throw new DateTimeParseException("Text cannot be parsed to a Period", text, 0);
    }

    private static int number(String digits, int negate) {
        return digits == null ? 0 : Math.multiplyExact(Integer.parseInt(digits), negate);
    }

    @Override
    public long get(TemporalUnit unit) {
        if (unit == ChronoUnit.YEARS) {
            return years;
        }
        if (unit == ChronoUnit.MONTHS) {
            return months;
        }
        if (unit == ChronoUnit.DAYS) {
            return days;
        }
        throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    @Override
    public List<TemporalUnit> getUnits() {
        return List.of(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS);
    }

    public boolean isZero() {
        return this == ZERO;
    }

    public boolean isNegative() {
        return years < 0 || months < 0 || days < 0;
    }

    public int getYears() {
        return years;
    }

    public int getMonths() {
        return months;
    }

    public int getDays() {
        return days;
    }

    public Period withYears(int years) {
        return of(years, months, days);
    }

    public Period withMonths(int months) {
        return of(years, months, days);
    }

    public Period withDays(int days) {
        return of(years, months, days);
    }

    public Period plus(TemporalAmount amountToAdd) {
        Period other = from(amountToAdd);
        return of(Math.addExact(years, other.years), Math.addExact(months, other.months),
                Math.addExact(days, other.days));
    }

    public Period plusYears(long yearsToAdd) {
        return of(Math.toIntExact(Math.addExact(years, yearsToAdd)), months, days);
    }

    public Period plusMonths(long monthsToAdd) {
        return of(years, Math.toIntExact(Math.addExact(months, monthsToAdd)), days);
    }

    public Period plusDays(long daysToAdd) {
        return of(years, months, Math.toIntExact(Math.addExact(days, daysToAdd)));
    }

    public Period minus(TemporalAmount amountToSubtract) {
        return plus(from(amountToSubtract).negated());
    }

    public Period minusYears(long yearsToSubtract) {
        return plusYears(-yearsToSubtract);
    }

    public Period minusMonths(long monthsToSubtract) {
        return plusMonths(-monthsToSubtract);
    }

    public Period minusDays(long daysToSubtract) {
        return plusDays(-daysToSubtract);
    }

    public Period multipliedBy(int scalar) {
        return of(Math.multiplyExact(years, scalar), Math.multiplyExact(months, scalar),
                Math.multiplyExact(days, scalar));
    }

    public Period negated() {
        return multipliedBy(-1);
    }

    public Period normalized() {
        long totalMonths = toTotalMonths();
        return of(Math.toIntExact(totalMonths / 12), (int) (totalMonths % 12), days);
    }

    public long toTotalMonths() {
        return years * 12L + months;
    }

    @Override
    public Temporal addTo(Temporal temporal) {
        if (months == 0) {
            if (years != 0) {
                temporal = temporal.plus(years, ChronoUnit.YEARS);
            }
        } else {
            long totalMonths = toTotalMonths();
            if (totalMonths != 0) {
                temporal = temporal.plus(totalMonths, ChronoUnit.MONTHS);
            }
        }
        if (days != 0) {
            temporal = temporal.plus(days, ChronoUnit.DAYS);
        }
        return temporal;
    }

    @Override
    public Temporal subtractFrom(Temporal temporal) {
        if (months == 0) {
            if (years != 0) {
                temporal = temporal.minus(years, ChronoUnit.YEARS);
            }
        } else {
            long totalMonths = toTotalMonths();
            if (totalMonths != 0) {
                temporal = temporal.minus(totalMonths, ChronoUnit.MONTHS);
            }
        }
        if (days != 0) {
            temporal = temporal.minus(days, ChronoUnit.DAYS);
        }
        return temporal;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Period other && years == other.years && months == other.months
                && days == other.days;
    }

    @Override
    public int hashCode() {
        return years + Integer.rotateLeft(months, 8) + Integer.rotateLeft(days, 16);
    }

    @Override
    public String toString() {
        if (this == ZERO) {
            return "P0D";
        }
        StringBuilder buf = new StringBuilder();
        buf.append('P');
        if (years != 0) {
            buf.append(years).append('Y');
        }
        if (months != 0) {
            buf.append(months).append('M');
        }
        if (days != 0) {
            buf.append(days).append('D');
        }
        return buf.toString();
    }
}
