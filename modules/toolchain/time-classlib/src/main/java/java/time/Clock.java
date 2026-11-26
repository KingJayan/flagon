package java.time;

public abstract class Clock {

    protected Clock() {
    }

    public static Clock systemUTC() {
        return new SystemClock(ZoneOffset.UTC);
    }

    public static Clock systemDefaultZone() {
        return new SystemClock(ZoneId.systemDefault());
    }

    public static Clock system(ZoneId zone) {
        return new SystemClock(zone);
    }

    public static Clock fixed(Instant fixedInstant, ZoneId zone) {
        return new FixedClock(fixedInstant, zone);
    }

    public static Clock offset(Clock baseClock, Duration offsetDuration) {
        return offsetDuration.isZero() ? baseClock : new OffsetClock(baseClock, offsetDuration);
    }

    public abstract ZoneId getZone();

    public abstract Clock withZone(ZoneId zone);

    public long millis() {
        return instant().toEpochMilli();
    }

    public abstract Instant instant();

    private static final class SystemClock extends Clock {
        private final ZoneId zone;

        SystemClock(ZoneId zone) {
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new SystemClock(zone);
        }

        @Override
        public long millis() {
            return System.currentTimeMillis();
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof SystemClock other && zone.equals(other.zone);
        }

        @Override
        public int hashCode() {
            return zone.hashCode() + 1;
        }

        @Override
        public String toString() {
            return "SystemClock[" + zone + "]";
        }
    }

    private static final class FixedClock extends Clock {
        private final Instant instant;
        private final ZoneId zone;

        FixedClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new FixedClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FixedClock other && instant.equals(other.instant)
                    && zone.equals(other.zone);
        }

        @Override
        public int hashCode() {
            return instant.hashCode() ^ zone.hashCode();
        }

        @Override
        public String toString() {
            return "FixedClock[" + instant + "," + zone + "]";
        }
    }

    private static final class OffsetClock extends Clock {
        private final Clock base;
        private final Duration offset;

        OffsetClock(Clock base, Duration offset) {
            this.base = base;
            this.offset = offset;
        }

        @Override
        public ZoneId getZone() {
            return base.getZone();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new OffsetClock(base.withZone(zone), offset);
        }

        @Override
        public Instant instant() {
            return base.instant().plus(offset);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OffsetClock other && base.equals(other.base)
                    && offset.equals(other.offset);
        }

        @Override
        public int hashCode() {
            return base.hashCode() ^ offset.hashCode();
        }

        @Override
        public String toString() {
            return "OffsetClock[" + base + "," + offset + "]";
        }
    }
}
