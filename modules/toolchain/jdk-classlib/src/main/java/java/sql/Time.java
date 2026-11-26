package java.sql;

/**
 * the clock part of a moment in time, which is what a time column holds.
 */
public class Time extends java.util.Date {

    public Time(long time) {
        super(time);
    }

    @SuppressWarnings("deprecation")
    public static Time valueOf(String clock) {
        String[] parts = clock.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "a time is written hh:mm:ss, got '" + clock + "'");
        }
        return new Time(new java.util.Date(70, 0, 1, Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]), Integer.parseInt(parts[2])).getTime());
    }

    @Override
    @SuppressWarnings("deprecation")
    public String toString() {
        return String.format("%02d:%02d:%02d", getHours(), getMinutes(), getSeconds());
    }
}
