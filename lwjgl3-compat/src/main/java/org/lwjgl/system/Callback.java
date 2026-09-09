package org.lwjgl.system;

import dev.flagon.lwjgl.internal.Addresses;
import java.nio.ByteBuffer;

/** the base of the callback objects glfw hands out, which here wrap a plain java object */
public abstract class Callback implements Pointer, AutoCloseable {

    private static final ByteBuffer HANDLES = ByteBuffer.allocateDirect(1);

    private boolean freed;

    protected Callback() {
    }

    @Override
    public long address() {
        return Addresses.of(HANDLES);
    }

    public boolean isFreed() {
        return freed;
    }

    /** nothing was allocated outside the heap, so this only marks the callback unusable */
    public void free() {
        freed = true;
    }

    @Override
    public void close() {
        free();
    }
}
