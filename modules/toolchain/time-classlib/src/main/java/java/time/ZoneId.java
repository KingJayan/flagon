package java.time;

import java.time.format.TextStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.time.zone.ZoneRules;
import java.util.Locale;
import java.util.Map;
import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.impl.unicode.Intl;

public abstract class ZoneId {

    public static final Map<String, String> SHORT_IDS = Map.ofEntries(
            Map.entry("ACT", "Australia/Darwin"),
            Map.entry("AET", "Australia/Sydney"),
            Map.entry("AGT", "America/Argentina/Buenos_Aires"),
            Map.entry("ART", "Africa/Cairo"),
            Map.entry("AST", "America/Anchorage"),
            Map.entry("BET", "America/Sao_Paulo"),
            Map.entry("BST", "Asia/Dhaka"),
            Map.entry("CAT", "Africa/Harare"),
            Map.entry("CNT", "America/St_Johns"),
            Map.entry("CST", "America/Chicago"),
            Map.entry("CTT", "Asia/Shanghai"),
            Map.entry("EAT", "Africa/Addis_Ababa"),
            Map.entry("ECT", "Europe/Paris"),
            Map.entry("IET", "America/Indiana/Indianapolis"),
            Map.entry("IST", "Asia/Kolkata"),
            Map.entry("JST", "Asia/Tokyo"),
            Map.entry("MIT", "Pacific/Apia"),
            Map.entry("NET", "Asia/Yerevan"),
            Map.entry("NST", "Pacific/Auckland"),
            Map.entry("PLT", "Asia/Karachi"),
            Map.entry("PNT", "America/Phoenix"),
            Map.entry("PRT", "America/Puerto_Rico"),
            Map.entry("PST", "America/Los_Angeles"),
            Map.entry("SST", "Pacific/Guadalcanal"),
            Map.entry("VST", "Asia/Ho_Chi_Minh"),
            Map.entry("EST", "-05:00"),
            Map.entry("MST", "-07:00"),
            Map.entry("HST", "-10:00"));

    ZoneId() {
    }

    public static ZoneId systemDefault() {
        return of(Intl.systemZone());
    }

    public static ZoneId of(String zoneId, Map<String, String> aliasMap) {
        String id = aliasMap.get(zoneId);
        return of(id != null ? id : zoneId);
    }

    public static ZoneId of(String zoneId) {
        if (zoneId.length() <= 1 || zoneId.startsWith("+") || zoneId.startsWith("-")) {
            return ZoneOffset.of(zoneId);
        }
        if (zoneId.startsWith("UTC") || zoneId.startsWith("GMT")) {
            return ofWithPrefix(zoneId, 3);
        }
        if (zoneId.startsWith("UT")) {
            return ofWithPrefix(zoneId, 2);
        }
        return ZoneRegion.ofId(zoneId);
    }

    public static ZoneId ofOffset(String prefix, ZoneOffset offset) {
        if (prefix.isEmpty()) {
            return offset;
        }
        if (!prefix.equals("GMT") && !prefix.equals("UTC") && !prefix.equals("UT")) {
            throw new IllegalArgumentException("prefix should be GMT, UTC or UT, is: " + prefix);
        }
        String id = offset.getTotalSeconds() != 0 ? prefix + offset.getId() : prefix;
        return new ZoneRegion(id, offset.getRules());
    }

    private static ZoneId ofWithPrefix(String zoneId, int prefixLength) {
        String prefix = zoneId.substring(0, prefixLength);
        if (zoneId.length() == prefixLength) {
            return ofOffset(prefix, ZoneOffset.UTC);
        }
        if (zoneId.charAt(prefixLength) != '+' && zoneId.charAt(prefixLength) != '-') {
            return ZoneRegion.ofId(zoneId);
        }
        try {
            return ofOffset(prefix, ZoneOffset.of(zoneId.substring(prefixLength)));
        } catch (DateTimeException e) {
            throw new DateTimeException("Invalid ID for offset-based ZoneId: " + zoneId, e);
        }
    }

    public static ZoneId from(TemporalAccessor temporal) {
        ZoneId zone = temporal.query(TemporalQueries.zone());
        if (zone == null) {
            throw new DateTimeException("Unable to obtain ZoneId from TemporalAccessor: "
                    + temporal + " of type " + temporal.getClass().getName());
        }
        return zone;
    }

    public abstract String getId();

    public abstract ZoneRules getRules();

    public String getDisplayName(TextStyle style, Locale locale) {
        String name = CLDRHelper.getTimeZoneName(locale.getLanguage(), locale.getCountry(),
                getId());
        if (style != TextStyle.FULL && style != TextStyle.FULL_STANDALONE) {
            String tag = CLDRHelper.tag(locale.getLanguage(), locale.getCountry());
            name = Intl.zoneName(tag, getId(), "shortGeneric", System.currentTimeMillis());
        }
        return name != null ? name : getId();
    }

    public ZoneId normalized() {
        ZoneRules rules = getRules();
        return rules.isFixedOffset() ? rules.getOffset(Instant.EPOCH) : this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ZoneId other && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return getId();
    }
}
