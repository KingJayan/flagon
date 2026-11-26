package java.time;

import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Duration implements TemporalAmount, Comparable<Duration> {

    public static final Duration ZERO = new Duration(0, 0);

    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final Pattern PATTERN = Pattern.compile("([-+]?)P(?:([-+]?[0-9]+)D)?"
            + "(T(?:([-+]?[0-9]+)H)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)(?:[.,]([0-9]{0,9}))?S)?)?",
            Pattern.CASE_INSENSITIVE);

    private final long seconds;
    private final int nanos;

    private Duration(long seconds, int nanos) {
        this.seconds = seconds;
        this.nanos = nanos;
    }

    private static Duration create(long seconds, int nanos) {
        return (seconds | nanos) == 0 ? ZERO : new Duration(seconds, nanos);
    }

    public static Duration ofDays(long days) {
        return create(Math.multiplyExact(days, 86400L), 0);
    }

    public static Duration ofHours(long hours) {
        return create(Math.multiplyExact(hours, 3600L), 0);
    }

    public static Duration ofMinutes(long minutes) {
        return create(Math.multiplyExact(minutes, 60L), 0);
    }

    public static Duration ofSeconds(long seconds) {
        return create(seconds, 0);
    }

    public static Duration ofSeconds(long seconds, long nanoAdjustment) {
        long secs = Math.addExact(seconds, Math.floorDiv(nanoAdjustment, NANOS_PER_SECOND));
        return create(secs, (int) Math.floorMod(nanoAdjustment, NANOS_PER_SECOND));
    }

    public static Duration ofMillis(long millis) {
        return create(Math.floorDiv(millis, 1000L), (int) Math.floorMod(millis, 1000L) * 1_000_000);
    }

    public static Duration ofNanos(long nanos) {
        return ofSeconds(0, nanos);
    }

    public static Duration of(long amount, TemporalUnit unit) {
        return ZERO.plus(amount, unit);
    }

    public static Duration from(TemporalAmount amount) {
        Duration duration = ZERO;
        for (TemporalUnit unit : amount.getUnits()) {
            duration = duration.plus(amount.get(unit), unit);
        }
        return duration;
    }

    public static Duration between(Temporal startInclusive, Temporal endExclusive) {
        try {
            return ofNanos(startInclusive.until(endExclusive, ChronoUnit.NANOS));
        } catch (DateTimeException | ArithmeticException e) {
            long secs = startInclusive.until(endExclusive, ChronoUnit.SECONDS);
            long nanos;
            try {
                nanos = endExclusive.getLong(ChronoField.NANO_OF_SECOND)
                        - startInclusive.getLong(ChronoField.NANO_OF_SECOND);
                if (secs > 0 && nanos < 0) {
                    secs++;
                } else if (secs < 0 && nanos > 0) {
                    secs--;
                }
            } catch (DateTimeException e2) {
                nanos = 0;
            }
            return ofSeconds(secs, nanos);
        }
    }

    public static Duration parse(CharSequence text) {
        Matcher matcher = PATTERN.matcher(text);
        if (matcher.matches() && !"T".equalsIgnoreCase(matcher.group(3))) {
            boolean negate = "-".equals(matcher.group(1));
            try {
                long days = number(text, matcher.group(2), 86400);
                long hours = number(text, matcher.group(4), 3600);
                long minutes = number(text, matcher.group(5), 60);
                long secs = number(text, matcher.group(6), 1);
                String fraction = matcher.group(7);
                long nanos = 0;
                if (fraction != null && !fraction.isEmpty()) {
                    nanos = Long.parseLong((fraction + "000000000").substring(0, 9));
                    if (matcher.group(6).startsWith("-")) {
                        nanos = -nanos;
                    }
                }
                Duration duration = ofSeconds(Math.addExact(days, Math.addExact(hours,
                        Math.addExact(minutes, secs))), nanos);
                return negate ? duration.negated() : duration;
            } catch (ArithmeticException e) {
                throw new DateTimeParseException("Text cannot be parsed to a Duration: overflow",
                        text, 0, e);
            }
        }
        throw new DateTimeParseException("Text cannot be parsed to a Duration", text, 0);
    }

    private static long number(CharSequence text, String digits, int multiplier) {
        if (digits == null) {
            return 0;
        }
        return Math.multiplyExact(Long.parseLong(digits), (long) multiplier);
    }

    @Override
    public long get(TemporalUnit unit) {
        if (unit == ChronoUnit.SECONDS) {
            return seconds;
        }
        if (unit == ChronoUnit.NANOS) {
            return nanos;
        }
        throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    @Override
    public List<TemporalUnit> getUnits() {
        return List.of(ChronoUnit.SECONDS, ChronoUnit.NANOS);
    }

    public boolean isZero() {
        return (seconds | nanos) == 0;
    }

    public boolean isNegative() {
        return seconds < 0;
    }

    public boolean isPositive() {
        return (seconds | nanos) != 0 && seconds >= 0;
    }

    public long getSeconds() {
        return seconds;
    }

    public int getNano() {
        return nanos;
    }

    public Duration withSeconds(long newSeconds) {
        return create(newSeconds, nanos);
    }

    public Duration withNanos(int nanoOfSecond) {
        ChronoField.NANO_OF_SECOND.checkValidIntValue(nanoOfSecond);
        return create(seconds, nanoOfSecond);
    }

    public Duration plus(Duration duration) {
        return plus(duration.seconds, duration.nanos);
    }

    public Duration plus(long amountToAdd, TemporalUnit unit) {
        if (unit == ChronoUnit.DAYS) {
            return plus(Math.multiplyExact(amountToAdd, 86400L), 0);
        }
        if (unit.isDurationEstimated()) {
            throw new UnsupportedTemporalTypeException("Unit must not have an estimated duration");
        }
        if (amountToAdd == 0) {
            return this;
        }
        if (unit instanceof ChronoUnit chrono) {
            switch (chrono) {
                case NANOS:
                    return plusNanos(amountToAdd);
                case MICROS:
                    return plusSeconds((amountToAdd / (1_000_000L * 1000)) * 1000)
                            .plusNanos((amountToAdd % (1_000_000L * 1000)) * 1000);
                case MILLIS:
                    return plusMillis(amountToAdd);
                case SECONDS:
                    return plusSeconds(amountToAdd);
                default:
                    return plusSeconds(Math.multiplyExact(unit.getDuration().seconds, amountToAdd));
            }
        }
        Duration duration = unit.getDuration().multipliedBy(amountToAdd);
        return plusSeconds(duration.getSeconds()).plusNanos(duration.getNano());
    }

    public Duration plusDays(long daysToAdd) {
        return plus(Math.multiplyExact(daysToAdd, 86400L), 0);
    }

    public Duration plusHours(long hoursToAdd) {
        return plus(Math.multiplyExact(hoursToAdd, 3600L), 0);
    }

    public Duration plusMinutes(long minutesToAdd) {
        return plus(Math.multiplyExact(minutesToAdd, 60L), 0);
    }

    public Duration plusSeconds(long secondsToAdd) {
        return plus(secondsToAdd, 0);
    }

    public Duration plusMillis(long millisToAdd) {
        return plus(millisToAdd / 1000, (millisToAdd % 1000) * 1_000_000);
    }

    public Duration plusNanos(long nanosToAdd) {
        return plus(0, nanosToAdd);
    }

    private Duration plus(long secondsToAdd, long nanosToAdd) {
        if ((secondsToAdd | nanosToAdd) == 0) {
            return this;
        }
        long epochSec = Math.addExact(seconds, secondsToAdd);
        epochSec = Math.addExact(epochSec, nanosToAdd / NANOS_PER_SECOND);
        nanosToAdd = nanosToAdd % NANOS_PER_SECOND;
        return ofSeconds(epochSec, nanos + nanosToAdd);
    }

    public Duration minus(Duration duration) {
        long secsToSubtract = duration.getSeconds();
        int nanosToSubtract = duration.getNano();
        if (secsToSubtract == Long.MIN_VALUE) {
            return plus(Long.MAX_VALUE, -nanosToSubtract).plus(1, 0);
        }
        return plus(-secsToSubtract, -nanosToSubtract);
    }

    public Duration minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE
                ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit);
    }

    public Duration minusDays(long daysToSubtract) {
        return plusDays(-daysToSubtract);
    }

    public Duration minusHours(long hoursToSubtract) {
        return plusHours(-hoursToSubtract);
    }

    public Duration minusMinutes(long minutesToSubtract) {
        return plusMinutes(-minutesToSubtract);
    }

    public Duration minusSeconds(long secondsToSubtract) {
        return plusSeconds(-secondsToSubtract);
    }

    public Duration minusMillis(long millisToSubtract) {
        return plusMillis(-millisToSubtract);
    }

    public Duration minusNanos(long nanosToSubtract) {
        return plusNanos(-nanosToSubtract);
    }

    public Duration multipliedBy(long multiplicand) {
        if (multiplicand == 0) {
            return ZERO;
        }
        if (multiplicand == 1) {
            return this;
        }
        long nanosTotal = Math.multiplyExact((long) nanos, multiplicand);
        return ofSeconds(Math.multiplyExact(seconds, multiplicand), 0).plusNanos(nanosTotal);
    }

    public Duration dividedBy(long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        if (divisor == 1) {
            return this;
        }
        long secs = seconds / divisor;
        long remainder = seconds % divisor;
        long nanosTotal = (remainder * NANOS_PER_SECOND + nanos) / divisor;
        return ofSeconds(secs, nanosTotal);
    }

    public long dividedBy(Duration divisor) {
        return toNanos() / divisor.toNanos();
    }

    public Duration negated() {
        return multipliedBy(-1);
    }

    public Duration abs() {
        return isNegative() ? negated() : this;
    }

    @Override
    public Temporal addTo(Temporal temporal) {
        if (seconds != 0) {
            temporal = temporal.plus(seconds, ChronoUnit.SECONDS);
        }
        if (nanos != 0) {
            temporal = temporal.plus(nanos, ChronoUnit.NANOS);
        }
        return temporal;
    }

    @Override
    public Temporal subtractFrom(Temporal temporal) {
        if (seconds != 0) {
            temporal = temporal.minus(seconds, ChronoUnit.SECONDS);
        }
        if (nanos != 0) {
            temporal = temporal.minus(nanos, ChronoUnit.NANOS);
        }
        return temporal;
    }

    public long toDays() {
        return seconds / 86400;
    }

    public long toHours() {
        return seconds / 3600;
    }

    public long toMinutes() {
        return seconds / 60;
    }

    public long toSeconds() {
        return seconds;
    }

    public long toMillis() {
        long tempSeconds = seconds;
        long tempNanos = nanos;
        if (tempSeconds < 0) {
            tempSeconds = tempSeconds + 1;
            tempNanos = tempNanos - NANOS_PER_SECOND;
        }
        return Math.addExact(Math.multiplyExact(tempSeconds, 1000), tempNanos / 1_000_000);
    }

    public long toNanos() {
        long tempSeconds = seconds;
        long tempNanos = nanos;
        if (tempSeconds < 0) {
            tempSeconds = tempSeconds + 1;
            tempNanos = tempNanos - NANOS_PER_SECOND;
        }
        return Math.addExact(Math.multiplyExact(tempSeconds, NANOS_PER_SECOND), tempNanos);
    }

    public long toDaysPart() {
        return seconds / 86400;
    }

    public int toHoursPart() {
        return (int) (toHours() % 24);
    }

    public int toMinutesPart() {
        return (int) (toMinutes() % 60);
    }

    public int toSecondsPart() {
        return (int) (seconds % 60);
    }

    public int toMillisPart() {
        return nanos / 1_000_000;
    }

    public int toNanosPart() {
        return nanos;
    }

    public Duration truncatedTo(TemporalUnit unit) {
        if (unit == ChronoUnit.SECONDS && (seconds >= 0 || nanos == 0)) {
            return create(seconds, 0);
        }
        if (unit == ChronoUnit.NANOS) {
            return this;
        }
        long unitNanos = unit.getDuration().toNanos();
        long nod = (seconds % 86400) * NANOS_PER_SECOND + nanos;
        long result = (nod / unitNanos) * unitNanos;
        return plusNanos(result - nod);
    }

    @Override
    public int compareTo(Duration other) {
        int cmp = Long.compare(seconds, other.seconds);
        return cmp != 0 ? cmp : nanos - other.nanos;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Duration duration && seconds == duration.seconds
                && nanos == duration.nanos;
    }

    @Override
    public int hashCode() {
        return ((int) (seconds ^ (seconds >>> 32))) + (51 * nanos);
    }

    @Override
    public String toString() {
        if (this == ZERO) {
            return "PT0S";
        }
        long effectiveTotalSecs = seconds;
        if (seconds < 0 && nanos > 0) {
            effectiveTotalSecs++;
        }
        long hours = effectiveTotalSecs / 3600;
        int minutes = (int) ((effectiveTotalSecs % 3600) / 60);
        int secs = (int) (effectiveTotalSecs % 60);
        StringBuilder buf = new StringBuilder(24);
        buf.append("PT");
        if (hours != 0) {
            buf.append(hours).append('H');
        }
        if (minutes != 0) {
            buf.append(minutes).append('M');
        }
        if (secs == 0 && nanos == 0 && buf.length() > 2) {
            return buf.toString();
        }
        if (seconds < 0 && nanos > 0) {
            if (secs == 0) {
                buf.append("-0");
            } else {
                buf.append(secs);
            }
        } else {
            buf.append(secs);
        }
        if (nanos > 0) {
            int pos = buf.length();
            if (seconds < 0) {
                buf.append(2 * NANOS_PER_SECOND - nanos);
            } else {
                buf.append(nanos + NANOS_PER_SECOND);
            }
            while (buf.charAt(buf.length() - 1) == '0') {
                buf.setLength(buf.length() - 1);
            }
            buf.setCharAt(pos, '.');
        }
        buf.append('S');
        return buf.toString();
    }
}
