/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
// flagon: locale data comes from the page's Intl, not from cldr data compiled into the bundle.
// only the default locale, the available locales and the week rules stay compiled in.
package org.teavm.classlib.impl.unicode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.platform.metadata.IntResource;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;
import org.teavm.platform.metadata.StringResource;

public final class CLDRHelper {
    private static final String[] STYLES = { "full", "long", "medium", "short" };
    /** 2001-01-01, when every zone keeps standard time in its winter */
    private static final double WINTER = 978307200000.0;

    // plain gets and puts: a lambda's apply can pause somewhere, and would make this all async
    private static final Map<String, Object> cache = new HashMap<>();

    private CLDRHelper() {
    }

    public static String getCode(String language, String country) {
        return !country.isEmpty() ? language + "-" + country : language;
    }

    /** @return the locale as a tag Intl takes, such as en-US, or und for the root locale */
    public static String tag(String language, String country) {
        String code = getCode(language, country);
        String key = "tag " + code;
        String tag = (String) cache.get(key);
        if (tag == null) {
            tag = code.isEmpty() ? "und" : Intl.canonicalTag(code);
            cache.put(key, tag);
        }
        return tag;
    }

    public static String getLikelySubtags(String localeCode) {
        String key = "likely " + localeCode;
        String subtags = (String) cache.get(key);
        if (subtags == null) {
            subtags = Intl.likelySubtags(tag(localeCode, "")).replace('-', '_');
            cache.put(key, subtags);
        }
        return subtags;
    }

    public static String resolveCountry(String language, String country) {
        if (country.isEmpty()) {
            String subtags = getLikelySubtags(language);
            int index = subtags.lastIndexOf('_');
            country = index > 0 ? subtags.substring(index + 1) : "";
        }
        return country;
    }

    public static String[] resolveEras(String language, String country) {
        return names(language, country, "era", "short");
    }

    public static String[] resolveAmPm(String language, String country) {
        return names(language, country, "dayPeriod", "short");
    }

    public static String[] resolveMonths(String language, String country) {
        return names(language, country, "month", "long");
    }

    public static String[] resolveShortMonths(String language, String country) {
        return names(language, country, "month", "short");
    }

    public static String[] resolveWeekdays(String language, String country) {
        return names(language, country, "weekday", "long");
    }

    public static String[] resolveShortWeekdays(String language, String country) {
        return names(language, country, "weekday", "short");
    }

    /**
     * @param field month (from january), weekday (from sunday), era or dayPeriod
     * @param width long, short or narrow
     */
    public static String[] names(String language, String country, String field, String width) {
        String tag = tag(language, country);
        String key = "names " + tag + " " + field + " " + width;
        String[] names = (String[]) cache.get(key);
        if (names == null) {
            names = Intl.names(tag, field, width);
            cache.put(key, names);
        }
        return names.clone();
    }

    public static String getTimeZoneName(String language, String country, String id) {
        return Intl.zoneName(tag(language, country), id, "long", WINTER);
    }

    public static String getDisplayName(String language, String country, String type, String code) {
        return code.isEmpty() ? null : Intl.displayName(tag(language, country), type, code);
    }

    public static String getCurrencySymbol(String language, String country, String code) {
        return Intl.currencySymbol(tag(language, country), code);
    }

    public static native StringResource getDefaultLocale();

    public static native ResourceArray<StringResource> getAvailableLocales();

    public static native ResourceMap<IntResource> getMinimalDaysInFirstWeek();

    public static native ResourceMap<IntResource> getFirstDayOfWeek();

    /**
     * @param dateStyle a java.text.DateFormat style, or -1 for no date
     * @return a SimpleDateFormat pattern of the locale's style
     */
    public static String resolveDatePattern(String language, String country, int dateStyle,
            int timeStyle) {
        String tag = tag(language, country);
        String key = "date " + tag + " " + dateStyle + " " + timeStyle;
        String pattern = (String) cache.get(key);
        if (pattern == null) {
            pattern = datePattern(tag, Intl.dateParts(tag, style(dateStyle), style(timeStyle)));
            cache.put(key, pattern);
        }
        return pattern;
    }

    private static String style(int style) {
        return style < 0 ? "" : STYLES[style];
    }

    private static String datePattern(String tag, String[] parts) {
        StringBuilder pattern = new StringBuilder();
        for (int i = 0; i + 1 < parts.length; i += 2) {
            String value = parts[i + 1];
            switch (parts[i]) {
                case "pattern":
                    return value;
                case "era":
                    pattern.append('G');
                    break;
                case "year":
                    pattern.append(value.length() == 2 ? "yy" : "y");
                    break;
                case "month":
                    if (Character.isDigit(value.charAt(0))) {
                        pattern.append(value.length() == 2 ? "MM" : "M");
                    } else {
                        pattern.append(value.equals(Intl.names(tag, "month", "long")[0]) ? "MMMM" : "MMM");
                    }
                    break;
                case "day":
                    pattern.append(value.length() == 2 ? "dd" : "d");
                    break;
                case "weekday":
                    pattern.append(value.equals(Intl.names(tag, "weekday", "long")[1]) ? "EEEE" : "EEE");
                    break;
                case "dayPeriod":
                    pattern.append('a');
                    break;
                case "hour":
                    pattern.append(value.equals("15") ? "HH" : value.length() == 2 ? "hh" : "h");
                    break;
                case "minute":
                    pattern.append(value.length() == 2 ? "mm" : "m");
                    break;
                case "second":
                    pattern.append(value.length() == 2 ? "ss" : "s");
                    break;
                case "fractionalSecond":
                    pattern.append("SSS");
                    break;
                case "timeZoneName":
                    pattern.append(value.length() > 5 ? "zzzz" : "z");
                    break;
                case "literal":
                    pattern.append(quote(value, "'"));
                    break;
                default:
                    break;
            }
        }
        return pattern.toString();
    }

    public static String resolveNumberFormat(String language, String country) {
        return numberPattern(numberParts(language, country, "decimal"));
    }

    public static String resolvePercentFormat(String language, String country) {
        return numberPattern(numberParts(language, country, "percent"));
    }

    public static String resolveCurrencyFormat(String language, String country) {
        return numberPattern(numberParts(language, country, "currency"));
    }

    /** @return grouping, decimal, percent, minus, exponent, infinity and nan */
    public static String[] resolveDecimalSymbols(String language, String country) {
        String[] decimal = numberParts(language, country, "decimal");
        return new String[] {
            part(decimal, "group", ","),
            part(decimal, "decimal", "."),
            part(numberParts(language, country, "percent"), "percentSign", "%"),
            part(decimal, "minusSign", "-"),
            part(numberParts(language, country, "scientific"), "exponentSeparator", "E"),
            part(decimal, "infinity", "∞"),
            part(decimal, "nan", "NaN")
        };
    }

    private static String[] numberParts(String language, String country, String style) {
        String tag = tag(language, country);
        String key = "number " + tag + " " + style;
        String[] parts = (String[]) cache.get(key);
        if (parts == null) {
            parts = Intl.numberParts(tag, style);
            cache.put(key, parts);
        }
        return parts;
    }

    private static String part(String[] parts, String type, String otherwise) {
        for (int i = 0; i + 1 < parts.length; i += 2) {
            if (parts[i].equals(type)) {
                return parts[i + 1];
            }
        }
        return otherwise;
    }

    private static String numberPattern(String[] parts) {
        StringBuilder prefix = new StringBuilder();
        StringBuilder suffix = new StringBuilder();
        List<Integer> groups = new ArrayList<>();
        boolean number = false;
        int minimum = 0;
        int maximum = 0;
        for (int i = 0; i + 1 < parts.length; i += 2) {
            String value = parts[i + 1];
            StringBuilder affix = number ? suffix : prefix;
            switch (parts[i]) {
                case "pattern":
                    return value;
                case "integer":
                    groups.add(value.length());
                    number = true;
                    break;
                case "group":
                case "decimal":
                case "fraction":
                    number = true;
                    break;
                case "percentSign":
                    affix.append('%');
                    break;
                case "currency":
                    affix.append('¤');
                    break;
                case "literal":
                    affix.append(quote(value, "#0123456789,.;%‰¤-E'"));
                    break;
                case "minimumFractionDigits":
                    minimum = Integer.parseInt(value);
                    break;
                case "maximumFractionDigits":
                    maximum = Integer.parseInt(value);
                    break;
                default:
                    break;
            }
        }
        StringBuilder pattern = new StringBuilder(prefix);
        if (groups.size() < 2) {
            pattern.append('0');
        } else {
            int primary = groups.get(groups.size() - 1);
            int secondary = groups.size() > 2 ? groups.get(groups.size() - 2) : primary;
            pattern.append("#,");
            if (secondary != primary) {
                repeat(pattern, '#', secondary).append(',');
            }
            repeat(pattern, '#', primary - 1).append('0');
        }
        if (maximum > 0) {
            repeat(pattern.append('.'), '0', minimum);
            repeat(pattern, '#', maximum - minimum);
        }
        return pattern.append(suffix).toString();
    }

    private static StringBuilder repeat(StringBuilder out, char c, int count) {
        for (int i = 0; i < count; i++) {
            out.append(c);
        }
        return out;
    }

    /** @return the text, quoted when it has a letter or one of the special characters */
    private static String quote(String text, String special) {
        boolean plain = true;
        for (int i = 0; i < text.length() && plain; i++) {
            char c = text.charAt(i);
            plain = !(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') && special.indexOf(c) < 0;
        }
        return plain ? text : "'" + text.replace("'", "''") + "'";
    }
}
