package dev.flagon.sandbox;

import java.text.Collator;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** java.text and java.time in several locales, which the jdk and the page's Intl agree on */
final class LocaleChecks {

    /** 2006-01-02T15:04:05Z, a monday */
    private static final long WHEN = 1136214245000L;
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private LocaleChecks() {
    }

    static List<String> run() {
        List<String> lines = new ArrayList<>();
        numbers(lines);
        dates(lines);
        names(lines);
        collation(lines);
        time(lines);
        zones(lines);
        formatting(lines);
        return lines;
    }

    private static void numbers(List<String> lines) {
        lines.add("number: us " + NumberFormat.getInstance(Locale.US).format(1234567.891));
        lines.add("number: de " + NumberFormat.getInstance(Locale.GERMANY).format(1234567.891));
        lines.add("number: percent " + NumberFormat.getPercentInstance(Locale.US).format(0.256));
        lines.add("number: currency " + NumberFormat.getCurrencyInstance(Locale.US).format(1234.5));
        DecimalFormatSymbols de = DecimalFormatSymbols.getInstance(Locale.GERMANY);
        lines.add("number: de symbols " + de.getDecimalSeparator() + de.getGroupingSeparator());
        lines.add("number: pattern " + new DecimalFormat("#,##0.00", de).format(1234.5));
    }

    private static void dates(List<String> lines) {
        Date date = new Date(WHEN);
        int[] styles = { DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL };
        for (int style : styles) {
            lines.add("date: us " + utc(DateFormat.getDateInstance(style, Locale.US)).format(date));
        }
        DateFormat german = utc(DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMANY));
        lines.add("date: de " + german.format(date));
        DateFormat french = utc(DateFormat.getDateInstance(DateFormat.FULL, Locale.FRANCE));
        lines.add("date: fr " + french.format(date));
        DateFormat time = utc(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMANY));
        lines.add("date: time " + time.format(date));
        DateFormat pattern = utc(new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US));
        lines.add("date: pattern " + pattern.format(date));
        DateFormatSymbols frenchSymbols = DateFormatSymbols.getInstance(Locale.FRANCE);
        lines.add("date: fr months " + frenchSymbols.getMonths()[0] + " "
                + frenchSymbols.getShortMonths()[1]);
        String[] germanWeekdays = DateFormatSymbols.getInstance(Locale.GERMANY).getWeekdays();
        lines.add("date: de monday " + germanWeekdays[Calendar.MONDAY]);
        String[] eras = DateFormatSymbols.getInstance(Locale.US).getEras();
        lines.add("date: eras " + Arrays.toString(eras));
    }

    private static DateFormat utc(DateFormat format) {
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    private static void names(List<String> lines) {
        lines.add("name: " + Locale.GERMANY.getDisplayCountry(Locale.US) + ", "
                + Locale.GERMAN.getDisplayLanguage(Locale.FRANCE) + ", "
                + Locale.JAPAN.getDisplayCountry(Locale.GERMANY));
        Currency euro = Currency.getInstance("EUR");
        lines.add("name: " + euro.getSymbol(Locale.GERMANY) + " " + euro.getDisplayName(Locale.US));
    }

    private static void collation(List<String> lines) {
        Collator collator = Collator.getInstance(Locale.US);
        List<String> words = new ArrayList<>(List.of("b", "A", "ä", "c", "a"));
        words.sort(collator);
        lines.add("collator: sorted " + words);
        collator.setStrength(Collator.PRIMARY);
        boolean primary = collator.equals("a", "Ä");
        collator.setStrength(Collator.SECONDARY);
        lines.add("collator: primary " + primary + ", secondary " + collator.equals("a", "ä")
                + ", case " + collator.equals("a", "A"));
    }

    private static void time(List<String> lines) {
        LocalDate date = LocalDate.of(2006, 1, 2);
        LocalDate leapDay = LocalDate.of(2024, 2, 29);
        lines.add("time: date " + date + " " + date.getDayOfWeek() + " " + date.getDayOfYear());
        lines.add("time: months " + LocalDate.of(2024, 1, 31).plusMonths(1) + " "
                + LocalDate.of(2023, 3, 31).minusMonths(1));
        lines.add("time: between " + ChronoUnit.DAYS.between(date, leapDay) + " "
                + Period.between(date, leapDay) + " "
                + ChronoUnit.MONTHS.between(date, LocalDate.of(2024, 2, 1)));
        lines.add("time: epoch " + LocalDate.ofEpochDay(-1) + " " + date.toEpochDay() + " "
                + LocalDate.of(-1, 12, 31) + " " + LocalDate.ofEpochDay(-800_000));
        LocalDate lastOfFebruary = LocalDate.of(2024, 2, 10)
                .with(TemporalAdjusters.lastDayOfMonth());
        LocalDate friday = date.with(TemporalAdjusters.next(DayOfWeek.FRIDAY));
        lines.add("time: adjusters " + lastOfFebruary + " " + friday);
        LocalDateTime dateTime = LocalDateTime.of(2006, 1, 2, 15, 4, 5, 123_000_000);
        lines.add("time: local " + dateTime + " " + LocalTime.of(15, 4) + " "
                + LocalTime.parse("23:59:59.5").plusSeconds(1));
        lines.add("time: hours " + ChronoUnit.HOURS.between(dateTime, dateTime.minusMinutes(150))
                + " " + Duration.between(dateTime, dateTime.plusDays(1).plusNanos(5)));
        Instant instant = Instant.ofEpochMilli(WHEN + 123);
        lines.add("time: instant " + instant + " " + Instant.ofEpochSecond(-1) + " "
                + Instant.parse("2006-01-02T15:04:05.5Z").toEpochMilli());
        lines.add("time: duration " + Duration.ofMillis(29_000_123) + " "
                + Duration.ofSeconds(-1, 5) + " " + Duration.parse("PT1H30M").toMinutes() + " "
                + Duration.ofDays(2).toHours());
        lines.add("time: period " + Period.parse("P1Y2M3W4D") + " "
                + Period.ofMonths(14).normalized());
        lines.add("time: compare " + date.isBefore(date.plusDays(1)) + " "
                + dateTime.compareTo(dateTime.minusNanos(1)) + " "
                + date.equals(LocalDate.parse("2006-01-02")));
    }

    private static void zones(List<String> lines) {
        Instant instant = Instant.ofEpochMilli(WHEN);
        lines.add("zone: " + ZonedDateTime.ofInstant(instant, NEW_YORK) + " "
                + instant.atZone(ZoneId.of("Asia/Kolkata")).getOffset());
        lines.add("zone: gap " + ZonedDateTime.of(LocalDateTime.of(2024, 3, 10, 2, 30), NEW_YORK));
        ZonedDateTime overlap = ZonedDateTime.of(LocalDateTime.of(2024, 11, 3, 1, 30), NEW_YORK);
        lines.add("zone: overlap " + overlap + " " + overlap.withLaterOffsetAtOverlap());
        ZonedDateTime night = ZonedDateTime.of(LocalDateTime.of(2024, 3, 10, 1, 30), NEW_YORK);
        lines.add("zone: across " + night.plusHours(1) + " " + night.minusHours(13).plusDays(1));
        lines.add("zone: offsets " + ZoneOffset.of("+05:30").getTotalSeconds() + " "
                + ZoneOffset.ofHoursMinutes(-3, -30) + " " + ZoneId.of("UTC") + " "
                + ZoneId.of("GMT+2") + " " + NEW_YORK.getRules().isDaylightSavings(instant));
        OffsetDateTime offset = OffsetDateTime.parse("2006-01-02T15:04:05+07:00");
        ZonedDateTime zoned = ZonedDateTime.parse("2006-01-02T10:04:05-05:00[America/New_York]");
        lines.add("zone: offset date time " + offset.toInstant() + " " + zoned.toInstant());
    }

    private static void formatting(List<String> lines) {
        LocalDate date = LocalDate.of(2006, 1, 2);
        LocalDateTime dateTime = date.atTime(15, 4, 5);
        ZonedDateTime zoned = dateTime.atZone(NEW_YORK);
        lines.add("format: iso " + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime) + " "
                + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(zoned) + " "
                + DateTimeFormatter.BASIC_ISO_DATE.format(date) + " "
                + DateTimeFormatter.ISO_ZONED_DATE_TIME.format(zoned));

        DateTimeFormatter longDate = DateTimeFormatter.ofPattern("EEEE, d MMMM uuuu HH:mm",
                Locale.US);
        DateTimeFormatter clock = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
        DateTimeFormatter french = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRANCE);
        DateTimeFormatter stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX '['VV']'");
        lines.add("format: pattern " + longDate.format(dateTime) + " | " + clock.format(dateTime)
                + " | " + french.format(date) + " | " + stamp.format(zoned));

        DateTimeFormatter dotted = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter packed = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        DateTimeFormatter month = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
        lines.add("format: parse " + LocalDate.parse("02.01.2006", dotted) + " "
                + LocalDate.parse("20060102", DateTimeFormatter.BASIC_ISO_DATE) + " "
                + LocalDateTime.parse("20060102150405", packed) + " "
                + LocalTime.parse("3:04 PM", clock) + " " + LocalDate.parse("Jan 2, 2006", month));

        DateTimeFormatter medium = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
        DateTimeFormatter full = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);
        DateTimeFormatter germanLong = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                .withLocale(Locale.GERMANY);
        DateTimeFormatter shortTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
        // browsers put a space before PM, where the jdk puts a narrow no-break space
        String time = shortTime.withLocale(Locale.US).format(dateTime).replace(' ', ' ');
        lines.add("format: localized " + medium.withLocale(Locale.US).format(date) + " | "
                + full.withLocale(Locale.US).format(date) + " | " + germanLong.format(date) + " | "
                + time);

        lines.add("format: names " + Month.JANUARY.getDisplayName(TextStyle.FULL, Locale.FRANCE)
                + " " + DayOfWeek.MONDAY.getDisplayName(TextStyle.FULL, Locale.GERMANY) + " "
                + Month.MARCH.getDisplayName(TextStyle.SHORT, Locale.US));
        for (String text : new String[] { "2006-13-01", "2023-02-29", "2006-01-02x" }) {
            try {
                LocalDate.parse(text);
                lines.add("format: error none for " + text);
            } catch (DateTimeParseException e) {
                lines.add("format: error " + e.getMessage() + " at " + e.getErrorIndex());
            }
        }
    }
}
