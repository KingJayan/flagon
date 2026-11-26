package dev.flagon.platform;

public interface ConsoleSink {

    enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    void write(Level level, String message, Throwable error);

    default void debug(String message) {
        write(Level.DEBUG, message, null);
    }

    default void info(String message) {
        write(Level.INFO, message, null);
    }

    default void warn(String message) {
        write(Level.WARN, message, null);
    }

    default void error(String message, Throwable error) {
        write(Level.ERROR, message, error);
    }
}
