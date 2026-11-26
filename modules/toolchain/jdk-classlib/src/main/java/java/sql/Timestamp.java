package java.sql;

public class Timestamp extends java.util.Date {

    private int nanos;

    public Timestamp(long time) {
        super(time - floorMod(time));
        this.nanos = (int) (floorMod(time) * 1000000);
    }

    public int getNanos() {
        return nanos;
    }

    public void setNanos(int value) {
        if (value < 0 || value > 999999999) {
            throw new IllegalArgumentException("a nanosecond count is 0..999999999, got " + value);
        }
        this.nanos = value;
    }

    @Override
    public long getTime() {
        return super.getTime() + nanos / 1000000;
    }

    @Override
    @SuppressWarnings("deprecation")
    public String toString() {
        return String.format("%04d-%02d-%02d %02d:%02d:%02d.%09d", getYear() + 1900,
                getMonth() + 1, getDate(), getHours(), getMinutes(), getSeconds(), nanos);
    }

    private static long floorMod(long time) {
        long rest = time % 1000;
        return rest < 0 ? rest + 1000 : rest;
    }
}
