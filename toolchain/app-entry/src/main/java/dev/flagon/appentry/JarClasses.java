package dev.flagon.appentry;

import dev.flagon.classfile.Classes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class JarClasses implements Classes {

    record Where(Path jar, String entry) {
    }

    private final Map<String, Where> carriers;

    JarClasses(Map<String, Where> carriers) {
        this.carriers = carriers;
    }

    @Override
    public byte[] read(String name) {
        Where where = carriers.get(name);
        if (where == null) {
            return null;
        }
        try (JarFile opened = new JarFile(where.jar().toFile())) {
            JarEntry entry = opened.getJarEntry(where.entry());
            try (var in = opened.getInputStream(entry)) {
                return in.readAllBytes();
            }
        } catch (IOException | RuntimeException unreadable) {
            return null;
        }
    }

    @Override
    public boolean has(String name) {
        if (carriers.containsKey(name)) {
            return true;
        }
        try {
            Class.forName(name, false, ClassLoader.getPlatformClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError missing) {
            return false;
        }
    }
}
