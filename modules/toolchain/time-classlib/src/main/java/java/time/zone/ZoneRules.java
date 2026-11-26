package java.time.zone;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.teavm.classlib.impl.unicode.Intl;

/** the offsets of a zone: fixed, or what Intl says a region keeps at each instant */
public final class ZoneRules {

    /** the instants javascript dates reach */
    private static final double LIMIT = 8.64e15;

    private final ZoneOffset fixed;
    private final String region;

    private ZoneRules(ZoneOffset fixed, String region) {
        this.fixed = fixed;
        this.region = region;
    }

    public static ZoneRules of(ZoneOffset offset) {
        return new ZoneRules(offset, null);
    }

    /** flagon: rules that ask Intl, or null for a region Intl does not know */
    public static ZoneRules ofRegion(String region) {
        if (Intl.offsetSeconds(region, System.currentTimeMillis()) == Integer.MIN_VALUE) {
            return null;
        }
        return new ZoneRules(null, region);
    }

    public boolean isFixedOffset() {
        return fixed != null;
    }

    public ZoneOffset getOffset(Instant instant) {
        return fixed != null ? fixed
                : ZoneOffset.ofTotalSeconds(offsetAt(instant.getEpochSecond()));
    }

    private int offsetAt(long epochSecond) {
        double millis = Math.max(-LIMIT, Math.min(LIMIT, epochSecond * 1000.0));
        int seconds = Intl.offsetSeconds(region, millis);
        return seconds == Integer.MIN_VALUE ? 0 : seconds;
    }

    // arrays and plain calls, no List: a List call can pause in a dev build, which makes every
    // getLong, and then every toString, async

    /** @return the transition near the local time, when it falls in a gap or an overlap */
    public ZoneOffsetTransition getTransition(LocalDateTime localDateTime) {
        if (fixed != null) {
            return null;
        }
        long local = localDateTime.toEpochSecond(ZoneOffset.UTC);
        int before = offsetAt(local - 86400);
        int after = offsetAt(local + 86400);
        if (before == after) {
            return null;
        }
        long low = local - 86400;
        long high = local + 86400;
        while (high - low > 1) {
            long middle = low + (high - low) / 2;
            if (offsetAt(middle) == before) {
                low = middle;
            } else {
                high = middle;
            }
        }
        long start = high + Math.min(before, after);
        long end = high + Math.max(before, after);
        if (local < start || local >= end) {
            return null;
        }
        return new ZoneOffsetTransition(high, ZoneOffset.ofTotalSeconds(before),
                ZoneOffset.ofTotalSeconds(after));
    }

    public ZoneOffset getOffset(LocalDateTime localDateTime) {
        if (fixed != null) {
            return fixed;
        }
        ZoneOffsetTransition transition = getTransition(localDateTime);
        if (transition != null) {
            return transition.getOffsetBefore();
        }
        long local = localDateTime.toEpochSecond(ZoneOffset.UTC);
        int before = offsetAt(local - 86400);
        return ZoneOffset.ofTotalSeconds(offsetAt(local - before) == before ? before
                : offsetAt(local + 86400));
    }

    public List<ZoneOffset> getValidOffsets(LocalDateTime localDateTime) {
        ZoneOffsetTransition transition = getTransition(localDateTime);
        if (transition == null) {
            return List.of(getOffset(localDateTime));
        }
        return transition.isGap() ? List.of()
                : List.of(transition.getOffsetBefore(), transition.getOffsetAfter());
    }

    public boolean isValidOffset(LocalDateTime localDateTime, ZoneOffset offset) {
        ZoneOffsetTransition transition = getTransition(localDateTime);
        return transition != null ? transition.isValidOffset(offset)
                : getOffset(localDateTime).equals(offset);
    }

    public ZoneOffset getStandardOffset(Instant instant) {
        if (fixed != null) {
            return fixed;
        }
        int year = LocalDateTime.ofEpochSecond(instant.getEpochSecond(), 0, ZoneOffset.UTC)
                .getYear();
        long january = LocalDateTime.of(year, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC);
        long july = LocalDateTime.of(year, 7, 1, 0, 0).toEpochSecond(ZoneOffset.UTC);
        return ZoneOffset.ofTotalSeconds(Math.min(offsetAt(january), offsetAt(july)));
    }

    public boolean isDaylightSavings(Instant instant) {
        return !getStandardOffset(instant).equals(getOffset(instant));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ZoneRules other && (fixed != null ? fixed.equals(other.fixed)
                : region.equals(other.region));
    }

    @Override
    public int hashCode() {
        return fixed != null ? fixed.hashCode() : region.hashCode();
    }

    @Override
    public String toString() {
        return fixed != null ? "FixedRules:" + fixed : "ZoneRules[" + region + "]";
    }
}
