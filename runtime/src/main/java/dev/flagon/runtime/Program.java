package dev.flagon.runtime;

/* the runtime's equivalent of a main class. */
public interface Program {

    String id();

    String description();

    void run(String[] arguments) throws Exception;
}
