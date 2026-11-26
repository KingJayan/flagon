package java.time;

import dev.flagon.time.Names;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Locale;

public enum Month implements TemporalAccessor, TemporalAdjuster {
    JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER,
    DECEMBER;

    private static final Month[] VALUES = values();

    public static Month of(int month) {
        if (month < 1 || month > 12) {
            throw new DateTimeException("Invalid value for MonthOfYear: " + month);
        }
        return VALUES[month - 1];
    }

    public static Month from(TemporalAccessor temporal) {
        if (temporal instanceof Month month) {
            return month;
        }
        return of(temporal.get(ChronoField.MONTH_OF_YEAR));
    }

    public int getValue() {
        return ordinal() + 1;
    }

    public String getDisplayName(TextStyle style, Locale locale) {
        return Names.of("month", style, locale)[ordinal()];
    }

    @Override
    public boolean isSupported(TemporalField field) {
        return field instanceof ChronoField ? field == ChronoField.MONTH_OF_YEAR
                : field != null && field.isSupportedBy(this);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field == ChronoField.MONTH_OF_YEAR) {
            return getValue();
        }
        if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    public Month plus(long months) {
        return VALUES[(int) Math.floorMod(ordinal() + months, 12L)];
    }

    public Month minus(long months) {
        return plus(-(months % 12));
    }

    public int length(boolean leapYear) {
        switch (this) {
            case FEBRUARY:
                return leapYear ? 29 : 28;
            case APRIL:
            case JUNE:
            case SEPTEMBER:
            case NOVEMBER:
                return 30;
            default:
                return 31;
        }
    }

    public int minLength() {
        return this == FEBRUARY ? 28 : length(false);
    }

    public int maxLength() {
        return this == FEBRUARY ? 29 : length(false);
    }

    public int firstDayOfYear(boolean leapYear) {
        int leap = leapYear && ordinal() > 1 ? 1 : 0;
        int day = 1;
        for (int i = 0; i < ordinal(); i++) {
            day += VALUES[i].length(false);
        }
        return day + leap;
    }

    public Month firstMonthOfQuarter() {
        return VALUES[(ordinal() / 3) * 3];
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.precision()) {
            return (R) ChronoUnit.MONTHS;
        }
        return TemporalAccessor.super.query(query);
    }

    @Override
    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.MONTH_OF_YEAR, getValue());
    }
}
