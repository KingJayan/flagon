package dev.flagon.platform;

/** channel from the runtime back to whatever embedded it. hosted programs never call this. */
public interface RuntimeHost {

    String[] arguments();

    void catalog(String[] ids, String[] descriptions);

    void status(String phase, String detail);

    void ready();

    void exited(int code);

    /** terminal: no further callbacks follow. */
    void fatal(String phase, Throwable error);
}
