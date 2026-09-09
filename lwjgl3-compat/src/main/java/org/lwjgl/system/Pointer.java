package org.lwjgl.system;

/** anything that has an address */
public interface Pointer {

    int POINTER_SIZE = 4;

    int BITS32 = 32;

    int BITS64 = 64;

    long address();
}
