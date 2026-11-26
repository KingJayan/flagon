package dev.flagon.time;

import java.time.format.TextStyle;
import java.util.Locale;
import org.teavm.classlib.impl.unicode.CLDRHelper;

/** names of months, weekdays, eras and halves of the day, from Intl */
public final class Names {

    private Names() {
    }

    /** @param field month, weekday, era or dayPeriod, as CLDRHelper.names takes */
    public static String[] of(String field, TextStyle style, Locale locale) {
        String width = switch (style) {
            case FULL, FULL_STANDALONE -> "long";
            case SHORT, SHORT_STANDALONE -> "short";
            default -> "narrow";
        };
        return CLDRHelper.names(locale.getLanguage(), locale.getCountry(), field, width);
    }
}
