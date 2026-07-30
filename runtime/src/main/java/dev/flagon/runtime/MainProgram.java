package dev.flagon.runtime;

/**
 * a {@link Program} that hands control to an application's own {@code main}.
 *
 * <p>the method is passed as a reference rather than looked up by name: there is no class
 * loading at runtime, so a name the compiler never saw would not be in the bundle at all.
 * a method reference is a real call site, which is what makes the application reachable.
 */
public final class MainProgram implements Program {

    /** an application entry point, matching the shape of {@code main}. */
    @FunctionalInterface
    public interface Main {
        void run(String[] arguments) throws Exception;
    }

    private final String id;
    private final String description;
    private final Main main;

    public MainProgram(String id, String description, Main main) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("a program needs an id");
        }
        if (main == null) {
            throw new IllegalArgumentException("a program needs a main method");
        }
        this.id = id;
        this.description = description == null ? id : description;
        this.main = main;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public void run(String[] arguments) throws Exception {
        main.run(arguments);
    }
}
