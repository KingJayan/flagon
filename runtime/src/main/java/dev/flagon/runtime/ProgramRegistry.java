package dev.flagon.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * programs a bundle can boot.
 * explicit registration rather than ServiceLoader: an aot compiler only keeps what's
 * statically reachable.
 */
public final class ProgramRegistry {

    private final Map<String, Program> byId = new LinkedHashMap<>();

    public ProgramRegistry register(Program program) {
        if (program == null) {
            throw new IllegalArgumentException("program must not be null");
        }
        String id = program.id();
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("program id must not be empty");
        }
        if (byId.put(id, program) != null) {
            throw new IllegalStateException("duplicate program id: " + id);
        }
        return this;
    }

    public Program find(String id) {
        return byId.get(id);
    }

    public List<Program> all() {
        return new ArrayList<>(byId.values());
    }

    public String ids() {
        return String.join(", ", byId.keySet());
    }

    public boolean isEmpty() {
        return byId.isEmpty();
    }
}
