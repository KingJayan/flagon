package java.sql;

public class Date extends java.util.Date {

    public Date(long time) {
        super(time);
    }

    @SuppressWarnings("deprecation")
    public static Date valueOf(String day) {
        String[] parts = day.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException("a date is written yyyy-mm-dd, got '" + day + "'");
        }
        return new Date(new java.util.Date(Integer.parseInt(parts[0]) - 1900,
                Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2])).getTime());
    }

    @Override
    @SuppressWarnings("deprecation")
    public String toString() {
        return String.format("%04d-%02d-%02d", getYear() + 1900, getMonth() + 1, getDate());
    }
}
