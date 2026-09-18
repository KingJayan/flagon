package dev.flagon.classfile;

public interface Classes {

    byte[] read(String name);

    // teavm supplies some classes with no file on the classpath
    boolean has(String name);
}
