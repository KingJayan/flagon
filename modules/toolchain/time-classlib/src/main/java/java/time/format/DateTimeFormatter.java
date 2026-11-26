package java.time.format;

import dev.flagon.time.Names;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.impl.unicode.Intl;

public final class DateTimeFormatter {

    private static final int NORMAL = 0;
    private static final int NOT_NEGATIVE = 1;
    private static final int EXCEEDS_PAD = 2;

    public static final DateTimeFormatter ISO_LOCAL_DATE = iso("ISO_LOCAL_DATE",
            new Value(ChronoField.YEAR, 4, 10, EXCEEDS_PAD), new Literal("-", false),
            new Value(ChronoField.MONTH_OF_YEAR, 2, 2, NOT_NEGATIVE), new Literal("-", false),
            new Value(ChronoField.DAY_OF_MONTH, 2, 2, NOT_NEGATIVE));
    public static final DateTimeFormatter ISO_LOCAL_TIME = iso("ISO_LOCAL_TIME",
            new Value(ChronoField.HOUR_OF_DAY, 2, 2, NOT_NEGATIVE), new Literal(":", false),
            new Value(ChronoField.MINUTE_OF_HOUR, 2, 2, NOT_NEGATIVE),
            new Optional(new Literal(":", false),
                    new Value(ChronoField.SECOND_OF_MINUTE, 2, 2, NOT_NEGATIVE),
                    new Optional(new Fraction(0, 9, true))));
    public static final DateTimeFormatter ISO_LOCAL_DATE_TIME = iso("ISO_LOCAL_DATE_TIME",
            join(ISO_LOCAL_DATE, new Literal("T", true), ISO_LOCAL_TIME));
    public static final DateTimeFormatter ISO_OFFSET_DATE = iso("ISO_OFFSET_DATE",
            join(ISO_LOCAL_DATE, new Offset("+HH:MM:ss", "Z")));
    public static final DateTimeFormatter ISO_OFFSET_TIME = iso("ISO_OFFSET_TIME",
            join(ISO_LOCAL_TIME, new Offset("+HH:MM:ss", "Z")));
    public static final DateTimeFormatter ISO_OFFSET_DATE_TIME = iso("ISO_OFFSET_DATE_TIME",
            join(ISO_LOCAL_DATE_TIME, new Offset("+HH:MM:ss", "Z")));
    public static final DateTimeFormatter ISO_ZONED_DATE_TIME = iso("ISO_ZONED_DATE_TIME",
            join(ISO_OFFSET_DATE_TIME, region()));
    public static final DateTimeFormatter ISO_DATE_TIME = iso("ISO_DATE_TIME",
            join(ISO_LOCAL_DATE_TIME, new Optional(new Offset("+HH:MM:ss", "Z"), region())));
    public static final DateTimeFormatter ISO_DATE = iso("ISO_DATE",
            join(ISO_LOCAL_DATE, new Optional(new Offset("+HH:MM:ss", "Z"))));
    public static final DateTimeFormatter ISO_TIME = iso("ISO_TIME",
            join(ISO_LOCAL_TIME, new Optional(new Offset("+HH:MM:ss", "Z"))));
    public static final DateTimeFormatter ISO_INSTANT = iso("ISO_INSTANT", new InstantText());
    public static final DateTimeFormatter BASIC_ISO_DATE = iso("BASIC_ISO_DATE",
            new Value(ChronoField.YEAR, 4, 4, NOT_NEGATIVE),
            new Value(ChronoField.MONTH_OF_YEAR, 2, 2, NOT_NEGATIVE),
            new Value(ChronoField.DAY_OF_MONTH, 2, 2, NOT_NEGATIVE),
            new Optional(new Offset("+HHMMss", "Z")));
    public static final DateTimeFormatter RFC_1123_DATE_TIME = new DateTimeFormatter(
            "RFC_1123_DATE_TIME", join(new Optional(compile("EEE, ")),
                    compile("d MMM yyyy HH:mm"),
                    new Element[] { new Optional(compile(":ss")), new Literal(" ", false),
                        new Offset("+HHMM", "GMT") }), Locale.US, null, null, null);

    private final String description;
    private final Element[] elements;
    private final Locale locale;
    private final ZoneId zone;
    private final FormatStyle dateStyle;
    private final FormatStyle timeStyle;

    private DateTimeFormatter(String description, Element[] elements, Locale locale, ZoneId zone,
            FormatStyle dateStyle, FormatStyle timeStyle) {
        this.description = description;
        this.elements = elements;
        this.locale = locale;
        this.zone = zone;
        this.dateStyle = dateStyle;
        this.timeStyle = timeStyle;
    }

    private static DateTimeFormatter iso(String name, Element... elements) {
        return new DateTimeFormatter(name, elements, Locale.US, null, null, null);
    }

    /** [Europe/Paris] after the offset, left out for a zone that is only an offset */
    private static Element region() {
        return new Optional(new Literal("[", false), new Zone(true), new Literal("]", false));
    }

    private static Element[] join(Object... parts) {
        List<Element> all = new ArrayList<>();
        for (Object part : parts) {
            if (part instanceof DateTimeFormatter formatter) {
                all.addAll(List.of(formatter.elements));
            } else if (part instanceof Element[] elements) {
                all.addAll(List.of(elements));
            } else {
                all.add((Element) part);
            }
        }
        return all.toArray(new Element[0]);
    }

    public static DateTimeFormatter ofPattern(String pattern) {
        return ofPattern(pattern, Locale.getDefault());
    }

    public static DateTimeFormatter ofPattern(String pattern, Locale locale) {
        return new DateTimeFormatter(pattern, compile(pattern), locale, null, null, null);
    }

    public static DateTimeFormatter ofLocalizedDate(FormatStyle dateStyle) {
        return localized(dateStyle, null);
    }

    public static DateTimeFormatter ofLocalizedTime(FormatStyle timeStyle) {
        return localized(null, timeStyle);
    }

    public static DateTimeFormatter ofLocalizedDateTime(FormatStyle dateTimeStyle) {
        return localized(dateTimeStyle, dateTimeStyle);
    }

    public static DateTimeFormatter ofLocalizedDateTime(FormatStyle dateStyle,
            FormatStyle timeStyle) {
        return localized(dateStyle, timeStyle);
    }

    private static DateTimeFormatter localized(FormatStyle dateStyle, FormatStyle timeStyle) {
        if (dateStyle == null && timeStyle == null) {
            throw new IllegalArgumentException("Either the date or time style must be non-null");
        }
        return new DateTimeFormatter("Localized(" + (dateStyle != null ? dateStyle : "") + ","
                + (timeStyle != null ? timeStyle : "") + ")", null, Locale.getDefault(), null,
                dateStyle, timeStyle);
    }

    public Locale getLocale() {
        return locale;
    }

    public DateTimeFormatter withLocale(Locale locale) {
        return this.locale.equals(locale) ? this
                : new DateTimeFormatter(description, elements, locale, zone, dateStyle, timeStyle);
    }

    public DateTimeFormatter localizedBy(Locale locale) {
        return withLocale(locale);
    }

    public ZoneId getZone() {
        return zone;
    }

    public DateTimeFormatter withZone(ZoneId zone) {
        return Objects.equals(this.zone, zone) ? this
                : new DateTimeFormatter(description, elements, locale, zone, dateStyle, timeStyle);
    }

    /** the elements to run: a localized formatter takes its pattern from Intl for its locale */
    private Element[] elements() {
        if (elements != null) {
            return elements;
        }
        int date = dateStyle != null ? dateStyle.ordinal() : -1;
        int time = timeStyle != null ? timeStyle.ordinal() : -1;
        String pattern = CLDRHelper.resolveDatePattern(locale.getLanguage(), locale.getCountry(),
                date, time);
        return compile(pattern);
    }

    public String format(TemporalAccessor temporal) {
        StringBuilder out = new StringBuilder(32);
        formatInto(temporal, out);
        return out.toString();
    }

    public void formatTo(TemporalAccessor temporal, Appendable appendable) {
        StringBuilder out = new StringBuilder(32);
        formatInto(temporal, out);
        try {
            appendable.append(out);
        } catch (IOException e) {
            throw new DateTimeException(e.getMessage(), e);
        }
    }

    // a StringBuilder, not an Appendable: Instant.toString formats, and an Appendable can pause,
    // which would make every toString async
    private void formatInto(TemporalAccessor temporal, StringBuilder out) {
        if (zone != null && temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
            temporal = ZonedDateTime.ofInstant(Instant.from(temporal), zone);
        }
        Context context = new Context(temporal, locale);
        for (Element element : elements()) {
            element.format(context, out);
        }
    }

    public TemporalAccessor parse(CharSequence text) {
        Parsed parsed = new Parsed();
        int position = 0;
        for (Element element : elements()) {
            position = element.parse(parsed, locale, text, position);
            if (position < 0) {
                int index = ~position;
                throw new DateTimeParseException("Text '" + abbreviate(text)
                        + "' could not be parsed at index " + index, text, index);
            }
        }
        if (position < text.length()) {
            throw new DateTimeParseException("Text '" + abbreviate(text)
                    + "' could not be parsed, unparsed text found at index " + position, text,
                            position);
        }
        try {
            parsed.resolve(zone);
        } catch (DateTimeException e) {
            throw new DateTimeParseException("Text '" + abbreviate(text) + "' could not be parsed: "
                    + e.getMessage(), text, 0, e);
        }
        return parsed;
    }

    public <T> T parse(CharSequence text, TemporalQuery<T> query) {
        TemporalAccessor parsed = parse(text);
        try {
            return query.queryFrom(parsed);
        } catch (DateTimeParseException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DateTimeParseException("Text '" + abbreviate(text) + "' could not be parsed: "
                    + e.getMessage(), text, 0, e);
        }
    }

    private static String abbreviate(CharSequence text) {
        String abbr = text.toString();
        return abbr.length() > 64 ? abbr.substring(0, 64) + "..." : abbr;
    }

    @Override
    public String toString() {
        return description;
    }

    /** the elements of a pattern such as yyyy-MM-dd HH:mm */
    private static Element[] compile(String pattern) {
        List<List<Element>> open = new ArrayList<>();
        open.add(new ArrayList<>());
        for (int pos = 0; pos < pattern.length(); pos++) {
            char cur = pattern.charAt(pos);
            List<Element> into = open.get(open.size() - 1);
            if (cur >= 'A' && cur <= 'Z' || cur >= 'a' && cur <= 'z') {
                int start = pos++;
                while (pos < pattern.length() && pattern.charAt(pos) == cur) {
                    pos++;
                }
                int count = pos - start;
                pos--;
                into.add(letter(cur, count));
            } else if (cur == '\'') {
                int start = pos++;
                for (; pos < pattern.length(); pos++) {
                    if (pattern.charAt(pos) == '\'') {
                        if (pos + 1 < pattern.length() && pattern.charAt(pos + 1) == '\'') {
                            pos++;
                        } else {
                            break;
                        }
                    }
                }
                if (pos >= pattern.length()) {
                    throw new IllegalArgumentException(
                            "Pattern ends with an incomplete string literal: "
                            + pattern);
                }
                String str = pattern.substring(start + 1, pos);
                into.add(new Literal(str.isEmpty() ? "'" : str.replace("''", "'"), false));
            } else if (cur == '[') {
                open.add(new ArrayList<>());
            } else if (cur == ']') {
                if (open.size() == 1) {
                    throw new IllegalArgumentException(
                            "Pattern invalid as it contains ] without previous [");
                }
                List<Element> section = open.remove(open.size() - 1);
                open.get(open.size() - 1).add(new Optional(section.toArray(new Element[0])));
            } else if (cur == '{' || cur == '}' || cur == '#') {
                throw new IllegalArgumentException("Pattern includes reserved character: '" + cur
                        + "'");
            } else {
                into.add(new Literal(String.valueOf(cur), false));
            }
        }
        while (open.size() > 1) {
            List<Element> section = open.remove(open.size() - 1);
            open.get(open.size() - 1).add(new Optional(section.toArray(new Element[0])));
        }
        List<Element> elements = open.get(0);
        reserve(elements);
        return elements.toArray(new Element[0]);
    }

    /** a variable-width number leaves room for the fixed-width numbers that follow it */
    private static void reserve(List<Element> elements) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i) instanceof Value value && value.min != value.max) {
                int reserved = 0;
                for (int j = i + 1; j < elements.size(); j++) {
                    if (elements.get(j) instanceof Value next && next.min == next.max) {
                        reserved += next.min;
                    } else if (elements.get(j) instanceof Fraction fraction
                            && fraction.min == fraction.max && !fraction.point) {
                        reserved += fraction.min;
                    } else {
                        break;
                    }
                }
                value.reserved = reserved;
            }
        }
    }

    private static Element letter(char cur, int count) {
        switch (cur) {
            case 'G':
                return new Text(ChronoField.ERA, textStyle(count));
            case 'u':
            case 'y':
                ChronoField year = cur == 'u' ? ChronoField.YEAR : ChronoField.YEAR_OF_ERA;
                if (count == 2) {
                    return new TwoDigitYear(year);
                }
                return new Value(year, count, 19, count < 4 ? NORMAL : EXCEEDS_PAD);
            case 'M':
            case 'L':
                if (count <= 2) {
                    return number(ChronoField.MONTH_OF_YEAR, count, 2, cur);
                }
                return new Text(ChronoField.MONTH_OF_YEAR, textStyle(count));
            case 'd':
                return number(ChronoField.DAY_OF_MONTH, count, 2, cur);
            case 'D':
                if (count > 3) {
                    throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                return new Value(ChronoField.DAY_OF_YEAR, count, 3, NOT_NEGATIVE);
            case 'E':
                return new Text(ChronoField.DAY_OF_WEEK, count <= 3 ? TextStyle.SHORT
                        : count == 4 ? TextStyle.FULL : TextStyle.NARROW);
            case 'a':
                if (count > 1) {
                    throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                return new Text(ChronoField.AMPM_OF_DAY, TextStyle.SHORT);
            case 'h':
                return number(ChronoField.CLOCK_HOUR_OF_AMPM, count, 2, cur);
            case 'K':
                return number(ChronoField.HOUR_OF_AMPM, count, 2, cur);
            case 'k':
                return number(ChronoField.CLOCK_HOUR_OF_DAY, count, 2, cur);
            case 'H':
                return number(ChronoField.HOUR_OF_DAY, count, 2, cur);
            case 'm':
                return number(ChronoField.MINUTE_OF_HOUR, count, 2, cur);
            case 's':
                return number(ChronoField.SECOND_OF_MINUTE, count, 2, cur);
            case 'S':
                return new Fraction(count, count, false);
            case 'n':
                return new Value(ChronoField.NANO_OF_SECOND, count, 19, NOT_NEGATIVE);
            case 'N':
                return new Value(ChronoField.NANO_OF_DAY, count, 19, NOT_NEGATIVE);
            case 'A':
                return new Value(ChronoField.MILLI_OF_DAY, count, 19, NOT_NEGATIVE);
            case 'V':
                if (count != 2) {
                    throw new IllegalArgumentException("Pattern letter count must be 2: " + cur);
                }
                return new Zone(false);
            case 'z':
                if (count > 4) {
                    throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                return new ZoneText(count == 4);
            case 'O':
                if (count != 1 && count != 4) {
                    throw new IllegalArgumentException("Pattern letter count must be 1 or 4: "
                            + cur);
                }
                return new GmtOffset(count == 4);
            case 'X':
            case 'x':
                if (count > 5) {
                    throw new IllegalArgumentException("Too many pattern letters: " + cur);
                }
                String[] patterns = { "+HHmm", "+HHMM", "+HH:MM", "+HHMMss", "+HH:MM:ss" };
                String[] zeros = { "+00", "+0000", "+00:00", "+0000", "+00:00" };
                return new Offset(patterns[count - 1], cur == 'X' ? "Z" : zeros[count - 1]);
            case 'Z':
                if (count < 4) {
                    return new Offset("+HHMM", "+0000");
                }
                if (count == 4) {
                    return new GmtOffset(true);
                }
                if (count == 5) {
                    return new Offset("+HH:MM:ss", "Z");
                }
                throw new IllegalArgumentException("Too many pattern letters: " + cur);
            default:
                throw new IllegalArgumentException("Unknown pattern letter: " + cur);
        }
    }

    private static Value number(ChronoField field, int count, int max, char letter) {
        if (count > max) {
            throw new IllegalArgumentException("Too many pattern letters: " + letter);
        }
        return new Value(field, count, count == 1 ? 19 : count, NOT_NEGATIVE);
    }

    private static TextStyle textStyle(int count) {
        return count <= 3 ? TextStyle.SHORT : count == 4 ? TextStyle.FULL : TextStyle.NARROW;
    }

    /** what a formatter formats: the temporal, and the locale for its names */
    private record Context(TemporalAccessor temporal, Locale locale) {

        long value(TemporalField field) {
            return temporal.getLong(field);
        }

        ZoneId zone() {
            ZoneId found = temporal.query(TemporalQueries.zoneId());
            if (found == null) {
                throw new DateTimeException("Unable to extract ZoneId from temporal " + temporal);
            }
            return found;
        }
    }

    private interface Element {

        void format(Context context, StringBuilder out);

        /** @return the position after the text it read, or the complement of where it failed */
        int parse(Parsed parsed, Locale locale, CharSequence text, int position);
    }

    private static final class Literal implements Element {
        private final String text;
        private final boolean ignoreCase;

        Literal(String text, boolean ignoreCase) {
            this.text = text;
            this.ignoreCase = ignoreCase;
        }

        @Override
        public void format(Context context, StringBuilder out) {
            out.append(text);
        }

        @Override
        public int parse(Parsed parsed, Locale locale, CharSequence text, int position) {
            if (position + this.text.length() > text.length()
                    || !text.toString().regionMatches(ignoreCase, position, this.text, 0,
                            this.text.length())) {
                return ~position;
            }
            return position + this.text.length();
        }
    }

    private static final class Value implements Element {
        private final ChronoField field;
        private final int min;
        private final int max;
        private final int sign;
        private int reserved;

        Value(ChronoField field, int min, int max, int sign) {
            this.field = field;
            this.min = min;
            this.max = max;
            this.sign = sign;
        }

        @Override
        public void format(Context context, StringBuilder out) {
            long value = context.value(field);
            String digits = value == Long.MIN_VALUE ? "9223372036854775808"
                    : Long.toString(Math.abs(value));
            if (digits.length() > max) {
                throw new DateTimeException("Field " + field + " cannot be printed as the value "
                        + value + " exceeds the maximum print width of " + max);
            }
            if (value < 0) {
                if (sign == NOT_NEGATIVE) {
                    throw new DateTimeException("Field " + field
                            + " cannot be printed as the value "
                            + value + " cannot be negative according to the SignStyle");
                }
                out.append('-');
            } else if (sign == EXCEEDS_PAD && min < 19 && digits.length() > min) {
                out.append('+');
            }
            for (int i = digits.length(); i < min; i++) {
                out.append('0');
            }
            out.append(digits);
        }

        @Override
        public int parse(Parsed parsed, Locale locale, CharSequence text, int position) {
            int pos = position;
            boolean negative = false;
            if (pos < text.length() && sign != NOT_NEGATIVE) {
                char c = text.charAt(pos);
                if (c == '-' || c == '+' && sign == EXCEEDS_PAD) {
                    negative = c == '-';
                    pos++;
                }
            }
            int available = 0;
            while (pos + available < text.length() && Character.isDigit(text.charAt(pos
                    + available))) {
                available++;
            }
            int take = Math.min(max, Math.max(min, available - reserved));
            if (take > available || take < min || take == 0) {
                return ~pos;
            }
            long value = 0;
            for (int i = 0; i < take; i++) {
                value = value * 10 + (text.charAt(pos + i) - '0');
            }
            if (!parsed.put(field, negative ? -value : value)) {
                return ~position;
            }
            return pos + take;
        }
    }

    /** yy: two digits, read as a year from 2000 to 2099 */
    private static final class TwoDigitYear implements Element {
        private final ChronoField field;

        TwoDigitYear(ChronoField field) {
            this.field = field;
        }

        @Override
        public void format(Context context, StringBuilder out) {
            long value = Math.floorMod(context.value(field), 100L);
            out.append(value < 10 ? "0" : "").append(value);
        }

        @Override
        public int parse(Parsed parsed, Locale locale, CharSequence text, int position) {
            if (position + 2 > text.length() || !Character.isDigit(text.charAt(position))
                    || !Character.isDigit(text.charAt(position + 1))) {
                return ~position;
            }
            int value = (text.charAt(position) - '0') * 10 + text.charAt(position + 1) - '0';
            return parsed.put(field, 2000 + value) ? position + 2 : ~position;
        }
    }

    private static final class Fraction implements Element {
        private final int min;
        private final int max;
        private final boolean point;

        Fraction(int min, int max, boolean point) {
            this.min = min;
            this.max = max;
            this.point = point;
        }

        @Override
        public void format(Context context, StringBuilder out) {
            long nanos = context.value(ChronoField.NANO_OF_SECOND);
            String digits = Long.toString(nanos + 1_000_000_000L).substring(1);
            int length = max;
            while (length > min && digits.charAt(length - 1) == '0') {
                length--;
            }
            if (length > 0) {
                if (point) {
                    out.append('.');
                }
                out.append(digits, 0, length);
            }
        }

        @Override
        public int parse(Parsed parsed, Locale locale, CharSequence text, int position) {
            int pos = position;
            if (point) {
                if (pos >= text.length() || text.charAt(pos) != '.' && text.charAt(pos) != ',') {
                    return min > 0 ? ~pos : pos;
                }
                pos++;
            }
            int digits = 0;
            long value = 0;
            while (digits < max && pos + digits < text.length()
                    && Character.isDigit(text.charAt(pos + digits))) {
                value = value * 10 + (text.charAt(pos + digits) - '0');
                digits++;
            }
            if (digits < min || digits == 0 && point) {
                return ~pos;
            }
            for (int i = digits; i < 9; i++) {
                value *= 10;
            }
            return parsed.put(ChronoField.NANO_OF_SECOND, value) ? pos + digits : ~position;
        }
    }

    private static final class Text implements Element {
        private final ChronoField field;
        private final TextStyle style;

        Text(ChronoField field, TextStyle style) {
            this.field = field;
            this.style = style;
        }

        private String[] names(Locale locale) {
            switch (field) {
                case MONTH_OF_YEAR:
                    return Names.of("month", style, locale);
                case DAY_OF_WEEK:
                    String[] fromSunday = Names.of("weekday", style, locale);
                    String[] fromMonday = new String[7];
                    for (int i = 0; i < 7; i++) {
                        fromMonday[i] = fromSunday[(i + 1) % 7];
                    }
                    return fromMonday;
                case AMPM_OF_DAY:
                    return Names.of("dayPeriod", style, locale);
                default:
                    return Names.of("era", style, locale);
            }
        }

        /** the value of the first name: months and weekdays count from 1 */
        private int first() {
            return field == ChronoField.MONTH_OF_YEAR || field == ChronoField.DAY_OF_WEEK ? 1 : 0;
        }

        @Override
        public void format(Context context, StringBuilder out) {
            out.append(names(context.locale())[(int) context.value(field) - first()]);
        }

        @Override
        public int parse(Parsed parsed, Locale locale, CharSequence text, int position) {
            String[] names = names(locale);
            int best = -1;
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                if (!name.isEmpty() && text.toString().startsWith(name, position)
                        && (best < 0 || name.length() > names[best].length())) {
                    best = i;
                }
            }
            if (best < 0) {
                return ~position;
            }
            return parsed.put(field, best + first()) ? position + names[best].length() : ~position;
        }
    }

    private static final class Offset implements Element {
        private final String noOffsetText;
        private final boolean colon;
        /** 0: none, 1: when not zero, 2: always */
        private final int minutes;
        private final int seconds;

        Offset(String pattern, String noOffsetText) {
            this.noOffsetText = noOffsetText;
            this.colon = pattern.contains(":");
            this.minutes = pattern.contains("MM") ? 2 : pattern.contains("mm") ? 1 : 0;
            this.seconds = pattern.contains("SS") ? 2 : pattern.contains("ss") ? 1 : 0;
        }

        @Override
        public void format(Context context, StringBuilder out) {
            int total = Math.toIntExact(context.value(ChronoField.OFFSET_SECONDS));
            if (total == 0) {
                out.append(noOffsetText);
                return;
            }
            int abs = Math.abs(total);
            int hours = abs / 3600;
            int mins = (abs / 60) % 60;
            int secs = abs % 60;
            out.append(total < 0 ? '-' : '+').append(hours < 10 ? "0" : "").append(hours);
            if (minutes == 2 || minutes == 1 && (mins > 0 || secs > 0)) {
                out.append(colon ? ":" : "").append(mins < 10 ? "0" : "").append(mins);
                if (seconds == 2 || seconds == 1 && secs > 0) {
                    out.append(colon ? ":" : "").append(secs < 10 ? "0" : "").append(secs);
                }
            }
        }

        @Override
        public int parse(Parsed parsed, Locale locale, CharSequence text, int position) {
            String str = text.toString();
            int after = position + noOffsetText.length();
            if (str.startsWith(noOffsetText, position)
                    && (after == str.length() || !Character.isDigit(str.charAt(after)))) {
                return parsed.put(ChronoField.OFFSET_SECONDS, 0) ? after : ~position;
            }
            if (position >= str.length()
                    || str.charAt(position) != '+' && str.charAt(position) != '-') {
                return ~position;
            }
            int negate = str.charAt(position) == '-' ? -1 : 1;
            int pos = position + 1;
            int[] parts = new int[3];
            int count = minutes == 0 ? 1 : seconds == 0 ? 2 : 3;
            for (int i = 0; i < count; i++) {
                int at = pos + (i > 0 && colon ? 1 : 0);
                boolean present = at + 2 <= str.length() && Character.isDigit(str.charAt(at))
                        && Character.isDigit(str.charAt(at + 1))
                        && (i == 0 || !colon || str.charAt(pos) == ':');
                int need = i == 0 ? 2 : i == 1 ? minutes : seconds;
                if (!present) {
                    if (need == 2) {
                        return ~pos;
                    }
                    break;
                }
                parts[i] = (str.charAt(at) - '0') * 10 + str.charAt(at + 1) - '0';
                pos = at + 2;
            }
            if (parts[0] > 18 || parts[1] > 59 || parts[2] > 59) {
                return ~position;
            }
            int total = negate * (parts[0] * 3600 + parts[1] * 60 + parts[2]);
            return parsed.put(ChronoField.OFFSET_SECONDS, total) ? pos : ~position;
        }
    }

    /** O and ZZZZ: GMT, GMT+8 or GMT+08:00 */
    private static final class GmtOffset implements Element {
        private final boolean full;

        GmtOffset(boolean full) {
            this.full = full;
        }

        @Override
        public void format(Context context, StringBuilder out) {
            int total = Math.toIntExact(context.value(ChronoField.OFFSET_SECONDS));
            out.append("GMT");
            if (total == 0) {
                return;
            }
            int abs = Math.abs(total);
            int hours = abs / 3600;
            int mins = (abs / 60) % 60;
            int secs = abs % 60;
            out.append(total < 0 ? '-' : '+');
            if (full) {
                out.append(hours < 10 ? "0" : "").append(hours).append(mins < 10 ? ":0"
                        : ":").append(mins);
                if (secs > 0) {
                    out.append(secs < 10 ? ":0" : ":").append(secs);
                }
            } else {
                out.append(hours);
                if (mins > 0 || secs > 0) {
                    out.append(mins < 10 ? ":0" : ":").append(mins);
                    if (secs > 0) {
                        out.append(secs < 10 ? ":0" : ":").append(secs);
                    }
                }
            }
        }

        @Override
        public int parse(Parsed parsed, Locale locale, CharSequence text, int position) {
            String str = text.toString();
            if (!str.startsWith("GMT", position)) {
                return ~position;
            }
            int pos = position + 3;
            if (pos >= str.length() || str.charAt(pos) != '+' && str.charAt(pos) != '-') {
                return parsed.put(ChronoField.OFFSET_SECONDS, 0) ? pos : ~position;
            }
            int negate = str.charAt(pos++) == '-' ? -1 : 1;
            int[] parts = new int[3];
            for (int i = 0; i < 3; i++) {
                if (i > 0) {
                    if (pos >= str.length() || str.charAt(pos) != ':') {
                        break;
                    }
                    pos++;
                }
                int start = pos;
                while (pos < str.length() && pos - start < 2
                        && Character.isDigit(str.charAt(pos))) {
                    parts[i] = parts[i] * 10 + str.charAt(pos++) - '0';
                }
                if (pos == start) {
                    return ~pos;
                }
            }
            int total = negate * (parts[0] * 3600 + parts[1] * 60 + parts[2]);
            return parsed.put(ChronoField.OFFSET_SECONDS, total) ? pos : ~position;
        }
    }

    /** VV, and the region of ISO_ZONED_DATE_TIME, which leaves out a plain offset */
    private static final class Zone implements Element {
        private final boolean regionOnly;

        Zone(boolean regionOnly) {
            this.regionOnly = regionOnly;
        }

        @Override
        public void format(Context context, StringBuilder out) {
            ZoneId zone = context.zone();
            if (regionOnly && zone instanceof ZoneOffset) {
                throw new DateTimeException("not a region: " + zone);
            }
            out.append(zone.getId());
        }

        @Override
        public int parse(Parsed parsed, Locale locale, CharSequence text, int position) {
            int end = position;
            while (end < text.length()
                    && "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789~/._+-:"
                    .indexOf(text.charAt(end)) >= 0) {
                end++;
            }
            try {
                parsed.zone = ZoneId.of(text.subSequence(position, end).toString());
                return end;
            } catch (DateTimeException e) {
                return ~position;
            }
        }
    }

    /** z and zzzz: the name of the zone in the locale */
    private static final class ZoneText implements Element {
        private final boolean full;

        ZoneText(boolean full) {
            this.full = full;
        }

        @Override
        public void format(Context context, StringBuilder out) {
            ZoneId zone = context.zone();
            TemporalAccessor temporal = context.temporal();
            double millis = temporal.isSupported(ChronoField.INSTANT_SECONDS)
                    ? temporal.getLong(ChronoField.INSTANT_SECONDS) * 1000.0
                            : System.currentTimeMillis();
            Locale locale = context.locale();
            String name = Intl.zoneName(CLDRHelper.tag(locale.getLanguage(), locale.getCountry()),
                    zone.getId(), full ? "long" : "short", millis);
            out.append(name != null ? name : zone.getId());
        }

        @Override
        public int parse(Parsed parsed, Locale locale, CharSequence text, int position) {
            int end = position;
            while (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
                end++;
            }
            try {
                parsed.zone = ZoneId.of(text.subSequence(position, end).toString(),
                        ZoneId.SHORT_IDS);
                return end;
            } catch (DateTimeException e) {
                return ~position;
            }
        }
    }

    /** ISO_INSTANT: utc, with seconds, and a fraction in groups of three digits */
    private static final class InstantText implements Element {

        @Override
        public void format(Context context, StringBuilder out) {
            long seconds = context.value(ChronoField.INSTANT_SECONDS);
            TemporalAccessor temporal = context.temporal();
            int nanos = temporal.isSupported(ChronoField.NANO_OF_SECOND)
                    ? temporal.get(ChronoField.NANO_OF_SECOND) : 0;
            LocalDateTime local = LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC);
            Context utc = new Context(local, context.locale());
            for (Element element : ISO_LOCAL_DATE.elements) {
                element.format(utc, out);
            }
            int hour = local.getHour();
            int minute = local.getMinute();
            int second = local.getSecond();
            out.append('T').append(hour < 10 ? "0" : "").append(hour).append(minute < 10 ? ":0"
                    : ":")
                    .append(minute).append(second < 10 ? ":0" : ":").append(second);
            if (nanos > 0) {
                out.append('.');
                if (nanos % 1_000_000 == 0) {
                    out.append(Integer.toString((nanos / 1_000_000) + 1000).substring(1));
                } else if (nanos % 1000 == 0) {
                    out.append(Integer.toString((nanos / 1000) + 1_000_000).substring(1));
                } else {
                    out.append(Integer.toString(nanos + 1_000_000_000).substring(1));
                }
            }
            out.append('Z');
        }

        @Override
        public int parse(Parsed parsed, Locale locale, CharSequence text, int position) {
            Parsed inner = new Parsed();
            int pos = position;
            for (Element element : ISO_OFFSET_DATE_TIME.elements) {
                pos = element.parse(inner, locale, text, pos);
                if (pos < 0) {
                    return pos;
                }
            }
            if (!inner.fields.containsKey(ChronoField.SECOND_OF_MINUTE)) {
                return ~pos;
            }
            try {
                inner.resolve(null);
                long seconds = inner.getLong(ChronoField.INSTANT_SECONDS);
                long nanos = inner.getLong(ChronoField.NANO_OF_SECOND);
                if (!parsed.put(ChronoField.INSTANT_SECONDS, seconds)
                        || !parsed.put(ChronoField.NANO_OF_SECOND, nanos)) {
                    return ~position;
                }
                return pos;
            } catch (DateTimeException e) {
                return ~position;
            }
        }
    }

    /** a section that formats only when its fields are there, and parses only when it can */
    private static final class Optional implements Element {
        private final Element[] elements;

        Optional(Element... elements) {
            this.elements = elements;
        }

        @Override
        public void format(Context context, StringBuilder out) {
            int length = out.length();
            try {
                for (Element element : elements) {
                    element.format(context, out);
                }
            } catch (DateTimeException e) {
                out.setLength(length);
            }
        }

        @Override
        public int parse(Parsed parsed, Locale locale, CharSequence text, int position) {
            Parsed saved = parsed.copy();
            int pos = position;
            for (Element element : elements) {
                pos = element.parse(parsed, locale, text, pos);
                if (pos < 0) {
                    parsed.restore(saved);
                    return position;
                }
            }
            return pos;
        }
    }
}
