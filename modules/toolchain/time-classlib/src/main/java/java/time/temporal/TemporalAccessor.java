package java.time.temporal;

public interface TemporalAccessor {

    boolean isSupported(TemporalField field);

    long getLong(TemporalField field);

    default ValueRange range(TemporalField field) {
        if (field instanceof ChronoField chrono) {
            if (isSupported(field)) {
                return chrono.range();
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    default int get(TemporalField field) {
        long value = getLong(field);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new UnsupportedTemporalTypeException("Invalid field " + field
                    + " for get() method, use getLong() instead");
        }
        return (int) value;
    }

    default <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.zoneId() || query == TemporalQueries.chronology()
                || query == TemporalQueries.precision()) {
            return null;
        }
        return query.queryFrom(this);
    }
}
