package dev.decanter.platform;

/**
 * services a host must provide for desktop java programs to run.
 * grows one capability per milestone; every method here is really implemented by every host.
 */
public interface Platform {

    String name();

    Clock clock();

    ConsoleSink console();

    RuntimeHost host();
}
