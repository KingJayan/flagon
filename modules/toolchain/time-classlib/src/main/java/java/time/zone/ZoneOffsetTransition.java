package java.time.zone;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class ZoneOffsetTransition implements Comparable<ZoneOffsetTransition> {

    private final long epochSecond;
    private final LocalDateTime transition;
    private final ZoneOffset offsetBefore;
    private final ZoneOffset offsetAfter;

    ZoneOffsetTransition(long epochSecond, ZoneOffset offsetBefore, ZoneOffset offsetAfter) {
        this.epochSecond = epochSecond;
        this.transition = LocalDateTime.ofEpochSecond(epochSecond, 0, offsetBefore);
        this.offsetBefore = offsetBefore;
        this.offsetAfter = offsetAfter;
    }

    public static ZoneOffsetTransition of(LocalDateTime transition, ZoneOffset offsetBefore,
            ZoneOffset offsetAfter) {
        if (offsetBefore.equals(offsetAfter)) {
            throw new IllegalArgumentException("Offsets must not be equal");
        }
        if (transition.getNano() != 0) {
            throw new IllegalArgumentException("Nano-of-second must be zero");
        }
        return new ZoneOffsetTransition(transition.toEpochSecond(offsetBefore), offsetBefore,
                offsetAfter);
    }

    public Instant getInstant() {
        return Instant.ofEpochSecond(epochSecond);
    }

    public long toEpochSecond() {
        return epochSecond;
    }

    public LocalDateTime getDateTimeBefore() {
        return transition;
    }

    public LocalDateTime getDateTimeAfter() {
        return transition.plusSeconds(getDurationSeconds());
    }

    public ZoneOffset getOffsetBefore() {
        return offsetBefore;
    }

    public ZoneOffset getOffsetAfter() {
        return offsetAfter;
    }

    public Duration getDuration() {
        return Duration.ofSeconds(getDurationSeconds());
    }

    private int getDurationSeconds() {
        return offsetAfter.getTotalSeconds() - offsetBefore.getTotalSeconds();
    }

    public boolean isGap() {
        return offsetAfter.getTotalSeconds() > offsetBefore.getTotalSeconds();
    }

    public boolean isOverlap() {
        return offsetAfter.getTotalSeconds() < offsetBefore.getTotalSeconds();
    }

    public boolean isValidOffset(ZoneOffset offset) {
        return !isGap() && (offsetBefore.equals(offset) || offsetAfter.equals(offset));
    }

    @Override
    public int compareTo(ZoneOffsetTransition other) {
        return Long.compare(epochSecond, other.epochSecond);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ZoneOffsetTransition d && epochSecond == d.epochSecond
                && offsetBefore.equals(d.offsetBefore) && offsetAfter.equals(d.offsetAfter);
    }

    @Override
    public int hashCode() {
        return transition.hashCode() ^ offsetBefore.hashCode()
                ^ Integer.rotateLeft(offsetAfter.hashCode(), 16);
    }

    @Override
    public String toString() {
        return "Transition[" + (isGap() ? "Gap" : "Overlap") + " at " + transition + offsetBefore
                + " to " + offsetAfter + "]";
    }
}
