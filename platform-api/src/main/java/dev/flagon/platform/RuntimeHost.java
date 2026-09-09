package dev.flagon.platform;

public interface RuntimeHost {

    String[] arguments();

    void catalog(String[] ids, String[] descriptions);

    void status(String phase, String detail);

    void ready();

    void exited(int code);

    /** terminal: no further callbacks follow. */
    void fatal(String phase, Throwable error);
}
