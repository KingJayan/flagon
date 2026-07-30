package dev.flagon.runtime;

/** a unit of work the runtime can boot into — the runtime's equivalent of a main class. */
public interface Program {

    String id();

    String description();

    /** programs that drive a frame loop hand it to the scheduler and return; they don't block. */
    void run(String[] arguments) throws Exception;
}
