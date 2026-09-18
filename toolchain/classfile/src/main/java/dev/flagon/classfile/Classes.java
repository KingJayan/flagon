package dev.flagon.classfile;

/** gives class files to a {@link Loadable} */
public interface Classes {

    byte[] read(String name);

    boolean has(String name);
}
