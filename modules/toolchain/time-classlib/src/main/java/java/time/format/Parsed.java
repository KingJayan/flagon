package java.time.format;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.HashMap;

/** the fields a formatter read, and the date, time and zone they resolve to */
final class Parsed implements TemporalAccessor {

    // a HashMap, not a Map: a call through Map can pause in a dev build
    final HashMap<TemporalField, Long> fields = new HashMap<>();
    ZoneId zone;
    private LocalDate date;
    private LocalTime time;

    Parsed copy() {
        Parsed copy = new Parsed();
        copy.fields.putAll(fields);
        copy.zone = zone;
        return copy;
    }

    void restore(Parsed saved) {
        fields.clear();
        fields.putAll(saved.fields);
        zone = saved.zone;
    }

    /** @return false when the field already holds a different value */
    boolean put(TemporalField field, long value) {
        Long old = fields.put(field, value);
        return old == null || old == value;
    }

    void resolve(ZoneId overrideZone) {
        if (zone == null) {
            zone = overrideZone;
        }
        Long yearOfEra = fields.remove(ChronoField.YEAR_OF_ERA);
        if (yearOfEra != null) {
            Long era = fields.remove(ChronoField.ERA);
            long year = era == null || era == 1 ? yearOfEra : 1 - yearOfEra;
            conflict(ChronoField.YEAR, year);
        }
        Long clockHourOfAmPm = fields.remove(ChronoField.CLOCK_HOUR_OF_AMPM);
        if (clockHourOfAmPm != null) {
            conflict(ChronoField.HOUR_OF_AMPM, clockHourOfAmPm == 12 ? 0 : clockHourOfAmPm);
        }
        Long amPm = fields.get(ChronoField.AMPM_OF_DAY);
        Long hourOfAmPm = fields.get(ChronoField.HOUR_OF_AMPM);
        if (amPm != null && hourOfAmPm != null) {
            fields.remove(ChronoField.AMPM_OF_DAY);
            fields.remove(ChronoField.HOUR_OF_AMPM);
            conflict(ChronoField.HOUR_OF_DAY, amPm * 12 + hourOfAmPm);
        }
        Long clockHourOfDay = fields.remove(ChronoField.CLOCK_HOUR_OF_DAY);
        if (clockHourOfDay != null) {
            conflict(ChronoField.HOUR_OF_DAY, clockHourOfDay == 24 ? 0 : clockHourOfDay);
        }
        Long year = fields.get(ChronoField.YEAR);
        Long month = fields.get(ChronoField.MONTH_OF_YEAR);
        Long day = fields.get(ChronoField.DAY_OF_MONTH);
        Long dayOfYear = fields.get(ChronoField.DAY_OF_YEAR);
        Long epochDay = fields.get(ChronoField.EPOCH_DAY);
        if (year != null && month != null && day != null) {
            date = LocalDate.of(ChronoField.YEAR.checkValidIntValue(year),
                    ChronoField.MONTH_OF_YEAR.checkValidIntValue(month),
                    ChronoField.DAY_OF_MONTH.checkValidIntValue(day));
        } else if (year != null && dayOfYear != null) {
            date = LocalDate.ofYearDay(ChronoField.YEAR.checkValidIntValue(year),
                    ChronoField.DAY_OF_YEAR.checkValidIntValue(dayOfYear));
        } else if (epochDay != null) {
            date = LocalDate.ofEpochDay(epochDay);
        }
        Long dayOfWeek = fields.get(ChronoField.DAY_OF_WEEK);
        if (date != null && dayOfWeek != null && date.getDayOfWeek().getValue() != dayOfWeek) {
            throw new DateTimeException("Conflict found: Field DayOfWeek "
                    + date.getDayOfWeek().getValue() + " differs from DayOfWeek " + dayOfWeek
                    + " derived from " + date);
        }
        Long hour = fields.get(ChronoField.HOUR_OF_DAY);
        Long nanoOfDay = fields.get(ChronoField.NANO_OF_DAY);
        if (hour != null) {
            time = LocalTime.of(ChronoField.HOUR_OF_DAY.checkValidIntValue(hour),
                    ChronoField.MINUTE_OF_HOUR.checkValidIntValue(or(ChronoField.MINUTE_OF_HOUR)),
                    ChronoField.SECOND_OF_MINUTE.checkValidIntValue(
                            or(ChronoField.SECOND_OF_MINUTE)),
                    ChronoField.NANO_OF_SECOND.checkValidIntValue(or(ChronoField.NANO_OF_SECOND)));
        } else if (nanoOfDay != null) {
            time = LocalTime.ofNanoOfDay(nanoOfDay);
        }
    }

    private long or(TemporalField field) {
        Long value = fields.get(field);
        return value != null ? value : 0;
    }

    private void conflict(TemporalField field, long value) {
        if (!put(field, value)) {
            throw new DateTimeException("Conflict found: " + field + " " + fields.get(field)
                    + " differs from " + field + " " + value);
        }
    }

    private ZoneId zoneOrOffset() {
        if (zone != null) {
            return zone;
        }
        Long offset = fields.get(ChronoField.OFFSET_SECONDS);
        return offset != null ? ZoneOffset.ofTotalSeconds(offset.intValue()) : null;
    }

    @Override
    public boolean isSupported(TemporalField field) {
        if (fields.containsKey(field)) {
            return true;
        }
        if (field instanceof ChronoField chrono) {
            if (chrono == ChronoField.INSTANT_SECONDS) {
                return date != null && time != null && zoneOrOffset() != null;
            }
            return chrono.isDateBased() && date != null || chrono.isTimeBased() && time != null;
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public long getLong(TemporalField field) {
        Long value = fields.get(field);
        if (value != null) {
            return value;
        }
        if (field == ChronoField.INSTANT_SECONDS && isSupported(field)) {
            Long offset = fields.get(ChronoField.OFFSET_SECONDS);
            LocalDateTime local = LocalDateTime.of(date, time);
            return offset != null
                    ? local.toEpochSecond(ZoneOffset.ofTotalSeconds(offset.intValue()))
                    : local.atZone(zone).toEpochSecond();
        }
        if (field instanceof ChronoField chrono) {
            if (chrono.isDateBased() && date != null) {
                return date.getLong(field);
            }
            if (chrono.isTimeBased() && time != null) {
                return time.getLong(field);
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.zoneId()) {
            return (R) zone;
        }
        if (query == TemporalQueries.zone()) {
            return (R) zoneOrOffset();
        }
        if (query == TemporalQueries.localDate()) {
            return (R) date;
        }
        if (query == TemporalQueries.localTime()) {
            return (R) time;
        }
        if (query == TemporalQueries.chronology() || query == TemporalQueries.precision()) {
            return null;
        }
        return query.queryFrom(this);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(64);
        buf.append(fields).append(',').append("ISO");
        if (zone != null) {
            buf.append(',').append(zone);
        }
        if (date != null || time != null) {
            buf.append(" resolved to ");
            if (date != null) {
                buf.append(date);
                if (time != null) {
                    buf.append('T').append(time);
                }
            } else {
                buf.append(time);
            }
        }
        return buf.toString();
    }
}
