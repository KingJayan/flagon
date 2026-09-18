package dev.flagon.classfile;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Loadable {

    private final Classes source;
    private final Map<String, Set<String>> methods = new HashMap<>();
    private final Map<String, String[]> parents = new HashMap<>();
    private final Set<String> asked = new HashSet<>();

    public Loadable(Classes source) {
        this.source = source;
    }

    public boolean test(String name) {
        Bytes code = read(name);
        if (code == null) {
            return false;
        }
        for (String referenced : code.classes()) {
            if (!source.has(referenced)) {
                return false;
            }
        }
        for (String[] call : code.calls()) {
            if (source.read(call[0]) != null && !declares(call[0], call[1])) {
                return false;
            }
        }
        return true;
    }

    private boolean declares(String owner, String member) {
        if (!asked.add(owner)) {
            return methods.getOrDefault(owner, Set.of()).contains(member)
                    || above(owner, member);
        }
        Bytes code = read(owner);
        methods.put(owner, code == null ? Set.of() : code.members());
        parents.put(owner, code == null ? new String[0] : code.parents());
        return methods.get(owner).contains(member) || above(owner, member);
    }

    private boolean above(String owner, String member) {
        for (String parent : parents.getOrDefault(owner, new String[0])) {
            if (source.read(parent) != null ? declares(parent, member) : source.has(parent)) {
                return true;
            }
        }
        return false;
    }

    private Bytes read(String name) {
        byte[] code = source.read(name);
        if (code == null) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(code))) {
            return Bytes.of(in);
        } catch (IOException | RuntimeException unreadable) {
            return null;
        }
    }
}
