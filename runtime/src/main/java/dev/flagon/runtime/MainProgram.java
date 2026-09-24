package dev.flagon.runtime;

public final class MainProgram implements Program {

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
