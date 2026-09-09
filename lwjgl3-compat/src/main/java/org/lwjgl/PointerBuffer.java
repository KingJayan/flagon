package org.lwjgl;

import dev.flagon.lwjgl.internal.Addresses;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import org.lwjgl.system.Pointer;

/** a buffer of addresses */
public final class PointerBuffer implements Pointer {

    private final LongBuffer addresses;

    private PointerBuffer(LongBuffer addresses) {
        this.addresses = addresses;
    }

    public static PointerBuffer allocateDirect(int capacity) {
        ByteBuffer bytes = ByteBuffer.allocateDirect(capacity * Long.BYTES);
        bytes.order(ByteOrder.nativeOrder());
        return create(bytes);
    }

    public static PointerBuffer create(ByteBuffer source) {
        return new PointerBuffer(Addresses.derive(source.asLongBuffer(), source, 0));
    }

    public int capacity() {
        return addresses.capacity();
    }

    public int position() {
        return addresses.position();
    }

    public PointerBuffer position(int position) {
        addresses.position(position);
        return this;
    }

    public int limit() {
        return addresses.limit();
    }

    public PointerBuffer limit(int limit) {
        addresses.limit(limit);
        return this;
    }

    public int remaining() {
        return addresses.remaining();
    }

    public boolean hasRemaining() {
        return addresses.hasRemaining();
    }

    public PointerBuffer clear() {
        addresses.clear();
        return this;
    }

    public PointerBuffer flip() {
        addresses.flip();
        return this;
    }

    public PointerBuffer rewind() {
        addresses.rewind();
        return this;
    }

    public long get() {
        return addresses.get();
    }

    public long get(int index) {
        return addresses.get(index);
    }

    public PointerBuffer put(long value) {
        addresses.put(value);
        return this;
    }

    public PointerBuffer put(int index, long value) {
        addresses.put(index, value);
        return this;
    }

    public PointerBuffer put(Pointer value) {
        return put(value.address());
    }

    public ByteBuffer getByteBuffer(int index, int size) {
        return Addresses.bytes(addresses.get(index), size);
    }

    @Override
    public long address() {
        return Addresses.of(addresses);
    }
}
