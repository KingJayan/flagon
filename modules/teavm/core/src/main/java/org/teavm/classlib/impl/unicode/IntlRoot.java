// flagon: what Intl answers under wasi, which has no javascript: en-US, and every zone is utc
package org.teavm.classlib.impl.unicode;

public final class IntlRoot {
    private static final String[] MONTHS = { "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December" };
    private static final String[] WEEKDAYS = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday",
            "Friday", "Saturday" };

    private IntlRoot() {
    }

    public static String canonicalTag(String tag) {
        return tag;
    }

    public static String likelySubtags(String tag) {
        return tag;
    }

    public static String[] names(String tag, String field, String width) {
        String[] names;
        switch (field) {
            case "month":
                names = MONTHS;
                break;
            case "weekday":
                names = WEEKDAYS;
                break;
            case "era":
                return width.equals("long") ? new String[] { "Before Christ", "Anno Domini" }
                        : new String[] { "BC", "AD" };
            default:
                return new String[] { "AM", "PM" };
        }
        String[] result = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            result[i] = width.equals("long") ? names[i]
                    : names[i].substring(0, width.equals("narrow") ? 1 : 3);
        }
        return result;
    }

    public static String[] dateParts(String tag, String dateStyle, String timeStyle) {
        String date = dateStyle.equals("full") ? "EEEE, MMMM d, y"
                : dateStyle.equals("long") ? "MMMM d, y"
                : dateStyle.equals("medium") ? "MMM d, y" : "M/d/yy";
        String time = timeStyle.equals("full") || timeStyle.equals("long") ? "h:mm:ss a z"
                : timeStyle.equals("medium") ? "h:mm:ss a" : "h:mm a";
        String pattern = dateStyle.isEmpty() ? time : timeStyle.isEmpty() ? date : date + ", " + time;
        return new String[] { "pattern", pattern };
    }

    public static String[] numberParts(String tag, String style) {
        String pattern = style.equals("percent") ? "#,##0%"
                : style.equals("currency") ? "¤#,##0.00" : "#,##0.###";
        return new String[] { "pattern", pattern, "group", ",", "decimal", ".", "percentSign", "%",
                "minusSign", "-", "exponentSeparator", "E", "infinity", "∞", "nan", "NaN" };
    }

    public static String currencySymbol(String tag, String code) {
        return code.equals("USD") ? "$" : code;
    }

    public static String displayName(String tag, String type, String code) {
        return null;
    }

    public static String zoneName(String tag, String zone, String style, double millis) {
        return zone.equals("UTC") ? (style.startsWith("long") ? "Coordinated Universal Time" : "UTC")
                : null;
    }

    public static int offsetSeconds(String zone, double millis) {
        return 0;
    }

    public static String systemZone() {
        return "UTC";
    }

    public static int compare(String tag, String sensitivity, String a, String b) {
        return sensitivity.equals("base") || sensitivity.equals("accent")
                ? a.compareToIgnoreCase(b) : a.compareTo(b);
    }
}
