// flagon: the page's Intl, where java.text and java.time read locale data; under wasi,
// WasiTransformation sends each public method to IntlRoot. each script is one expression, so
// it compiles to webassembly calls and needs no eval
package org.teavm.classlib.impl.unicode;

import java.util.HashMap;
import java.util.Map;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSString;

public final class Intl {
    private static final Map<String, JSObject> formats = new HashMap<>();

    private Intl() {
    }

    interface Part extends JSObject {
        @JSProperty
        String getType();

        @JSProperty
        String getValue();
    }

    interface Formatter extends JSObject {
        JSArrayReader<Part> formatToParts(double value);

        String format(double value);

        Options resolvedOptions();
    }

    interface Options extends JSObject {
        @JSProperty
        int getMinimumFractionDigits();

        @JSProperty
        int getMaximumFractionDigits();

        @JSProperty
        String getTimeZone();
    }

    interface Names extends JSObject {
        JSObject of(String code);
    }

    interface Collator extends JSObject {
        int compare(String a, String b);
    }

    interface LocaleTag extends JSObject {
        LocaleTag maximize();

        @JSProperty
        String getBaseName();
    }

    @JSBody(script = "return Intl.DateTimeFormat;")
    private static native JSObject dateTimeFormat();

    @JSBody(script = "return Intl.NumberFormat;")
    private static native JSObject numberFormat();

    @JSBody(script = "return Intl.DisplayNames;")
    private static native JSObject displayNames();

    @JSBody(script = "return Intl.Collator;")
    private static native JSObject collator();

    @JSBody(script = "return Intl.Locale;")
    private static native JSObject locale();

    private static JSObject type(String name) {
        switch (name) {
            case "DateTimeFormat":
                return dateTimeFormat();
            case "NumberFormat":
                return numberFormat();
            case "DisplayNames":
                return displayNames();
            case "Collator":
                return collator();
            default:
                return locale();
        }
    }

    @JSBody(params = { "type", "args" }, script = "return Reflect.construct(type, args);")
    private static native JSObject construct(JSObject type, JSArray<JSObject> args);

    @JSBody(script = "return {};")
    private static native JSObject object();

    @JSBody(params = { "target", "key", "value" }, script = "Reflect.set(target, key, value);")
    private static native void set(JSObject target, String key, String value);

    @JSBody(params = "tag", script = "return Intl.getCanonicalLocales(tag);")
    private static native JSArrayReader<JSString> canonical(String tag);

    /** @param options key, value, key, value */
    private static JSObject make(String type, String tag, String... options) {
        JSObject settings = object();
        for (int i = 0; i + 1 < options.length; i += 2) {
            set(settings, options[i], options[i + 1]);
        }
        JSArray<JSObject> args = JSArray.create();
        args.push(JSString.valueOf(tag));
        args.push(settings);
        return construct(type(type), args);
    }

    private static Formatter formatter(String type, String tag, String... options) {
        StringBuilder key = new StringBuilder(type).append(' ').append(tag);
        for (String option : options) {
            key.append(' ').append(option);
        }
        return cached(key.toString(), type, tag, options).cast();
    }

    // a plain get and put: a lambda's apply can pause somewhere, and would make this all async
    private static JSObject cached(String key, String type, String tag, String... options) {
        JSObject found = formats.get(key);
        if (found == null) {
            found = make(type, tag, options);
            formats.put(key, found);
        }
        return found;
    }

    private static String part(JSArrayReader<Part> parts, String type) {
        for (int i = 0; i < parts.getLength(); i++) {
            if (parts.get(i).getType().equals(type)) {
                return parts.get(i).getValue();
            }
        }
        return "";
    }

    private static String[] flatten(JSArrayReader<Part> parts, int extra) {
        String[] out = new String[parts.getLength() * 2 + extra];
        for (int i = 0; i < parts.getLength(); i++) {
            out[i * 2] = parts.get(i).getType();
            out[i * 2 + 1] = parts.get(i).getValue();
        }
        return out;
    }

    /** @return the epoch millis of a utc date, by the proleptic gregorian calendar */
    private static double utc(int year, int month, int day, int hour) {
        int y = month <= 2 ? year - 1 : year;
        int era = Math.floorDiv(y, 400);
        int yearOfEra = y - era * 400;
        int dayOfYear = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
        int dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear;
        long days = era * 146097L + dayOfEra - 719468;
        return (days * 24 + hour) * 3600000.0;
    }

    /** @return the tag as Intl writes it, or und when Intl refuses it */
    public static String canonicalTag(String tag) {
        try {
            JSArrayReader<JSString> tags = canonical(tag);
            return tags.getLength() > 0 ? tags.get(0).stringValue() : "und";
        } catch (RuntimeException refused) {
            return "und";
        }
    }

    /** @return the tag with its likely script and region, such as en-Latn-US */
    public static String likelySubtags(String tag) {
        try {
            JSArray<JSObject> args = JSArray.create();
            args.push(JSString.valueOf(tag));
            return construct(locale(), args).<LocaleTag>cast().maximize().getBaseName();
        } catch (RuntimeException refused) {
            return tag;
        }
    }

    /**
     * @param field month (from january), weekday (from sunday), era (bc, ad) or dayPeriod
     *     (am, pm)
     * @param width long, short or narrow
     */
    public static String[] names(String tag, String field, String width) {
        double[] at;
        Formatter format;
        switch (field) {
            case "month":
                at = new double[12];
                for (int m = 0; m < 12; m++) {
                    at[m] = utc(2001, m + 1, 15, 0);
                }
                format = formatter("DateTimeFormat", tag, "timeZone", "UTC", "month", width, "day",
                        "numeric");
                break;
            case "weekday":
                at = new double[7];
                for (int d = 0; d < 7; d++) {
                    at[d] = utc(2001, 1, 7 + d, 0);
                }
                format = formatter("DateTimeFormat", tag, "timeZone", "UTC", "weekday", width);
                break;
            case "era":
                at = new double[] { utc(-100, 1, 1, 0), utc(2001, 1, 1, 0) };
                format = formatter("DateTimeFormat", tag, "timeZone", "UTC", "era", width, "year",
                        "numeric");
                break;
            default:
                at = new double[] { utc(2001, 1, 1, 1), utc(2001, 1, 1, 13) };
                format = formatter("DateTimeFormat", tag, "timeZone", "UTC", "hour", "numeric",
                        "hourCycle", "h12");
                break;
        }
        String[] names = new String[at.length];
        for (int i = 0; i < at.length; i++) {
            names[i] = part(format.formatToParts(at[i]), field);
        }
        return names;
    }

    /**
     * @param dateStyle full, long, medium, short or empty for no date
     * @return type and value, in turn, of each part of 2006-01-02 15:04:05 utc
     */
    public static String[] dateParts(String tag, String dateStyle, String timeStyle) {
        Formatter format;
        if (dateStyle.isEmpty()) {
            format = formatter("DateTimeFormat", tag, "timeZone", "UTC", "numberingSystem", "latn",
                    "timeStyle", timeStyle);
        } else if (timeStyle.isEmpty()) {
            format = formatter("DateTimeFormat", tag, "timeZone", "UTC", "numberingSystem", "latn",
                    "dateStyle", dateStyle);
        } else {
            format = formatter("DateTimeFormat", tag, "timeZone", "UTC", "numberingSystem", "latn",
                    "dateStyle", dateStyle, "timeStyle", timeStyle);
        }
        return flatten(format.formatToParts(utc(2006, 1, 2, 15) + 245000), 0);
    }

    /**
     * @param style decimal, percent, currency or scientific
     * @return type and value, in turn, of each part of a negative number, then
     *     minimumFractionDigits, maximumFractionDigits, infinity and nan with their values
     */
    public static String[] numberParts(String tag, String style) {
        Formatter format;
        if (style.equals("scientific")) {
            format = formatter("NumberFormat", tag, "numberingSystem", "latn", "notation", style);
        } else if (style.equals("currency")) {
            format = formatter("NumberFormat", tag, "numberingSystem", "latn", "style", style,
                    "currency", "USD");
        } else {
            format = formatter("NumberFormat", tag, "numberingSystem", "latn", "style", style);
        }
        JSArrayReader<Part> parts = format.formatToParts(style.equals("percent") ? -12345.67
                : -1234567.891);
        String[] out = flatten(parts, 8);
        int at = parts.getLength() * 2;
        Options resolved = format.resolvedOptions();
        out[at] = "minimumFractionDigits";
        out[at + 1] = String.valueOf(resolved.getMinimumFractionDigits());
        out[at + 2] = "maximumFractionDigits";
        out[at + 3] = String.valueOf(resolved.getMaximumFractionDigits());
        out[at + 4] = "infinity";
        out[at + 5] = format.format(Double.POSITIVE_INFINITY);
        out[at + 6] = "nan";
        out[at + 7] = format.format(Double.NaN);
        return out;
    }

    /** @return the symbol of a currency in a locale, such as $ for USD in en-US */
    public static String currencySymbol(String tag, String code) {
        try {
            Formatter format = formatter("NumberFormat", tag, "style", "currency", "currency", code);
            String symbol = part(format.formatToParts(1), "currency");
            return symbol.isEmpty() ? code : symbol;
        } catch (RuntimeException refused) {
            return code;
        }
    }

    /**
     * @param type language, region or currency
     * @return the name of the code in the locale, or null when Intl has none
     */
    public static String displayName(String tag, String type, String code) {
        try {
            Names names = cached("DisplayNames " + tag + " " + type, "DisplayNames", tag, "type",
                    type, "fallback", "none").cast();
            JSObject name = names.of(code);
            return JSObjects.isUndefined(name) ? null : name.<JSString>cast().stringValue();
        } catch (RuntimeException refused) {
            return null;
        }
    }

    /**
     * @param style long, short, longGeneric or shortGeneric
     * @return the name of the zone at the instant, or null for an unknown zone
     */
    public static String zoneName(String tag, String zone, String style, double millis) {
        try {
            Formatter format = formatter("DateTimeFormat", tag, "timeZone", zone, "timeZoneName",
                    style);
            String name = part(format.formatToParts(millis), "timeZoneName");
            return name.isEmpty() ? null : name;
        } catch (RuntimeException refused) {
            return null;
        }
    }

    /** @return the zone's offset from utc at the instant, or Integer.MIN_VALUE for no such zone */
    public static int offsetSeconds(String zone, double millis) {
        String name;
        try {
            Formatter format = formatter("DateTimeFormat", "en-US", "timeZone", zone,
                    "timeZoneName", "longOffset");
            name = part(format.formatToParts(millis), "timeZoneName");
        } catch (RuntimeException refused) {
            return Integer.MIN_VALUE;
        }
        // GMT, or GMT+05:30, GMT-08:00, GMT+05:45:12, with a minus sign or a hyphen
        int sign = name.indexOf('+') >= 0 ? 1 : -1;
        int at = Math.max(name.indexOf('+'), Math.max(name.indexOf('-'), name.indexOf('−')));
        if (at < 0) {
            return 0;
        }
        String[] fields = name.substring(at + 1).split(":");
        int seconds = 0;
        for (int i = 0; i < fields.length && i < 3; i++) {
            seconds = seconds * 60 + Integer.parseInt(fields[i]);
        }
        for (int i = fields.length; i < 3; i++) {
            seconds *= 60;
        }
        return sign * seconds;
    }

    /** @return the zone the page runs in, such as Europe/Berlin */
    public static String systemZone() {
        String zone = formatter("DateTimeFormat", "en-US").resolvedOptions().getTimeZone();
        return zone != null ? zone : "UTC";
    }

    /** @param sensitivity base, accent, case or variant */
    public static int compare(String tag, String sensitivity, String a, String b) {
        Collator collator = cached("Collator " + tag + " " + sensitivity, "Collator", tag,
                "sensitivity", sensitivity).cast();
        return collator.compare(a, b);
    }
}
