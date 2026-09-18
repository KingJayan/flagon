package dev.flagon.classfile;

public interface Classes {

    byte[] read(String name);

    // true for a class teavm supplies, which no file on the classpath carries
    boolean has(String name);
}
