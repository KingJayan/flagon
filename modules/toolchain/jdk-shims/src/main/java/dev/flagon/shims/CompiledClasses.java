package dev.flagon.shims;

import dev.flagon.classfile.Classes;

import org.teavm.model.ClassReaderSource;

import java.io.IOException;
import java.io.InputStream;

final class CompiledClasses implements Classes {

    private final ClassLoader loader;
    private final ClassReaderSource source;

    CompiledClasses(ClassLoader loader, ClassReaderSource source) {
        this.loader = loader;
        this.source = source;
    }

    @Override
    public byte[] read(String name) {
        try (InputStream in = loader.getResourceAsStream(name.replace('.', '/') + ".class")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException unreadable) {
            return null;
        }
    }

    @Override
    public boolean has(String name) {
        return source.get(name) != null;
    }
}
