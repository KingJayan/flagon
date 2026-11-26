package java.time.temporal;

import java.time.DateTimeException;

public final class ValueRange {

    private final long minimum;
    private final long maximum;

    private ValueRange(long minimum, long maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public static ValueRange of(long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException("Minimum value must be less than maximum value");
        }
        return new ValueRange(min, max);
    }

    public long getMinimum() {
        return minimum;
    }

    public long getMaximum() {
        return maximum;
    }

    public boolean isIntValue() {
        return minimum >= Integer.MIN_VALUE && maximum <= Integer.MAX_VALUE;
    }

    public boolean isValidValue(long value) {
        return value >= minimum && value <= maximum;
    }

    public boolean isValidIntValue(long value) {
        return isIntValue() && isValidValue(value);
    }

    public long checkValidValue(long value, TemporalField field) {
        if (!isValidValue(value)) {
            throw new DateTimeException("Invalid value for " + field + " (valid values " + this
                    + "): " + value);
        }
        return value;
    }

    public int checkValidIntValue(long value, TemporalField field) {
        return (int) checkValidValue(value, field);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ValueRange other && minimum == other.minimum
                && maximum == other.maximum;
    }

    @Override
    public int hashCode() {
        long hash = minimum + (maximum << 16) + (maximum >> 48);
        return (int) (hash ^ (hash >>> 32));
    }

    @Override
    public String toString() {
        return minimum + " - " + maximum;
    }
}
