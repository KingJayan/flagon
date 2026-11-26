package java.time.temporal;

import java.time.DayOfWeek;

public final class TemporalAdjusters {

    private TemporalAdjusters() {
    }

    public static TemporalAdjuster firstDayOfMonth() {
        return temporal -> temporal.with(ChronoField.DAY_OF_MONTH, 1);
    }

    public static TemporalAdjuster lastDayOfMonth() {
        return temporal -> temporal.with(ChronoField.DAY_OF_MONTH,
                temporal.range(ChronoField.DAY_OF_MONTH).getMaximum());
    }

    public static TemporalAdjuster firstDayOfNextMonth() {
        return temporal -> temporal.with(ChronoField.DAY_OF_MONTH, 1).plus(1, ChronoUnit.MONTHS);
    }

    public static TemporalAdjuster firstDayOfYear() {
        return temporal -> temporal.with(ChronoField.DAY_OF_YEAR, 1);
    }

    public static TemporalAdjuster lastDayOfYear() {
        return temporal -> temporal.with(ChronoField.DAY_OF_YEAR,
                temporal.range(ChronoField.DAY_OF_YEAR).getMaximum());
    }

    public static TemporalAdjuster firstDayOfNextYear() {
        return temporal -> temporal.with(ChronoField.DAY_OF_YEAR, 1).plus(1, ChronoUnit.YEARS);
    }

    public static TemporalAdjuster next(DayOfWeek dayOfWeek) {
        return relative(dayOfWeek, 1, false);
    }

    public static TemporalAdjuster nextOrSame(DayOfWeek dayOfWeek) {
        return relative(dayOfWeek, 1, true);
    }

    public static TemporalAdjuster previous(DayOfWeek dayOfWeek) {
        return relative(dayOfWeek, -1, false);
    }

    public static TemporalAdjuster previousOrSame(DayOfWeek dayOfWeek) {
        return relative(dayOfWeek, -1, true);
    }

    private static TemporalAdjuster relative(DayOfWeek dayOfWeek, int direction, boolean same) {
        int wanted = dayOfWeek.getValue();
        return temporal -> {
            int day = temporal.get(ChronoField.DAY_OF_WEEK);
            if (same && day == wanted) {
                return temporal;
            }
            int days = direction > 0 ? Math.floorMod(wanted - day - 1, 7) + 1
                    : -(Math.floorMod(day - wanted - 1, 7) + 1);
            return temporal.plus(days, ChronoUnit.DAYS);
        };
    }
}
