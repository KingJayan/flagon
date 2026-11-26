package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.BufferUsage;
import dev.flagon.platform.graphics.GraphicsDevice;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * points attribute slots at their data, uploading anything that lives in client memory. a draw
 * first says what each array needs, so arrays interleaved in one client buffer share one upload.
 */
public final class StagingArrays {

    /** upload every element */
    public static final int WHOLE = -1;

    private static final long HASH_BASE = 0xcbf29ce484222325L;
    private static final long HASH_PRIME = 0x100000001b3L;

    private final GraphicsDevice device;
    private final int[] buffers;
    private final int[] sizes;
    private final Object[] scratch;
    private final long[] uploaded;

    /** one client buffer's part in the coming draw, uploaded into the staging buffer at index */
    private static final class Needed {
        final int index;
        int fromElement;
        int toElement;
        boolean sent;

        Needed(int index, int fromElement, int toElement) {
            this.index = index;
            this.fromElement = fromElement;
            this.toElement = toElement;
        }
    }

    private final Map<Buffer, Needed> needed = new IdentityHashMap<>();

    public StagingArrays(GraphicsDevice device, int slots) {
        this.device = device;
        this.buffers = new int[slots];
        this.sizes = new int[slots];
        this.scratch = new Object[slots];
        this.uploaded = new long[slots];
    }

    /** forgets what the last draw needed */
    public void begin() {
        needed.clear();
    }

    /** the vertices a draw reads from an array in client memory; call before binding it */
    public void need(ClientArray array, int first, int count) {
        Buffer client = array.client();
        if (array.buffer() != GraphicsDevice.NO_BUFFER || client == null) {
            return;
        }
        int elementSize = Buffers.elementSize(client);
        int fromElement = 0;
        int toElement = client.limit();
        if (count != WHOLE) {
            int fromByte = array.offset() + first * array.stride();
            int toByte = array.offset() + (first + count - 1) * array.stride()
                    + array.components() * array.type().sizeInBytes();
            // whole elements only
            fromElement = Math.max(0, fromByte / elementSize);
            toElement = Math.min(toElement, (toByte + elementSize - 1) / elementSize);
        }
        Needed known = needed.get(client);
        if (known == null) {
            needed.put(client, new Needed(needed.size(), fromElement, toElement));
        } else {
            known.fromElement = Math.min(known.fromElement, fromElement);
            known.toElement = Math.max(known.toElement, toElement);
        }
    }

    /**
     * @param base the vertex index 0 reads, folded into the offset so a quad draw needs no
     *             index rewrite
     */
    public void bind(int slot, ClientArray array, int base) {
        if (array.buffer() != GraphicsDevice.NO_BUFFER) {
            device.bindBuffer(BufferTarget.ARRAY, array.buffer());
        } else {
            Needed part = needed.get(array.client());
            if (part == null) {
                throw new IllegalStateException("slot " + slot + " was bound before its draw "
                        + "said what it needs");
            }
            device.bindBuffer(BufferTarget.ARRAY, buffer(part.index));
            if (!part.sent) {
                part.sent = true;
                upload(part.index, array.client(), part.fromElement, part.toElement);
            }
        }
        int offset = array.offset() + base * array.stride();
        if (array.isInteger()) {
            device.integerAttributePointer(slot, array.components(), array.type(), array.stride(),
                    offset);
        } else {
            device.attributePointer(slot, array.components(), array.type(), array.isNormalized(),
                    array.stride(), offset);
        }
    }

    private int buffer(int slot) {
        if (buffers[slot] == GraphicsDevice.NO_BUFFER) {
            buffers[slot] = device.createBuffer();
            sizes[slot] = 0;
            uploaded[slot] = 0L;
        }
        return buffers[slot];
    }

    private void upload(int slot, Buffer client, int fromElement, int toElement) {
        int elementSize = Buffers.elementSize(client);
        int size = client.limit() * elementSize;
        // a resize empties the buffer, so the next upload has to run
        boolean emptied = sizes[slot] != size;
        if (emptied) {
            device.bufferData(BufferTarget.ARRAY, size, BufferUsage.STREAM);
            sizes[slot] = size;
        }
        if (toElement <= fromElement) {
            return;
        }
        // floats and bytes go straight to the device
        int elements = toElement - fromElement;
        int offset = fromElement * elementSize;
        if (client instanceof FloatBuffer floats) {
            float[] data = floats(slot, elements);
            floats.get(fromElement, data, 0, elements);
            if (skip(slot, hash(data, elements, offset), emptied)) {
                return;
            }
            device.bufferSubData(BufferTarget.ARRAY, offset, data, elements);
        } else if (client instanceof ByteBuffer raw) {
            byte[] data = bytes(slot, elements);
            raw.get(fromElement, data, 0, elements);
            if (skip(slot, hash(data, elements, offset), emptied)) {
                return;
            }
            device.bufferSubData(BufferTarget.ARRAY, offset, data, elements);
        } else {
            throw new IllegalArgumentException(
                    "unsupported buffer type " + client.getClass().getName());
        }
    }

    /** the client buffer is written where this class cannot see, so only the bytes can tell */
    private boolean skip(int slot, long fingerprint, boolean emptied) {
        if (!emptied && uploaded[slot] == fingerprint) {
            return true;
        }
        uploaded[slot] = fingerprint;
        return false;
    }

    private static long hash(float[] data, int elements, int offset) {
        long mixed = seed(elements, offset);
        for (int at = 0; at < elements; at++) {
            mixed = (mixed ^ Float.floatToRawIntBits(data[at])) * HASH_PRIME;
        }
        return mixed;
    }

    private static long hash(byte[] data, int elements, int offset) {
        long mixed = seed(elements, offset);
        for (int at = 0; at < elements; at++) {
            mixed = (mixed ^ data[at]) * HASH_PRIME;
        }
        return mixed;
    }

    /** the same bytes at another offset are another upload */
    private static long seed(int elements, int offset) {
        return ((HASH_BASE ^ elements) * HASH_PRIME ^ offset) * HASH_PRIME;
    }

    private float[] floats(int slot, int needed) {
        if (scratch[slot] instanceof float[] held && held.length >= needed) {
            return held;
        }
        float[] grown = new float[room(scratch[slot], needed)];
        scratch[slot] = grown;
        return grown;
    }

    private byte[] bytes(int slot, int needed) {
        if (scratch[slot] instanceof byte[] held && held.length >= needed) {
            return held;
        }
        byte[] grown = new byte[room(scratch[slot], needed)];
        scratch[slot] = grown;
        return grown;
    }

    private static int room(Object held, int needed) {
        int had = 0;
        if (held instanceof float[] floats) {
            had = floats.length;
        } else if (held instanceof byte[] bytes) {
            had = bytes.length;
        }
        return Math.max(needed, had * 2);
    }

    public void dispose() {
        for (int slot = 0; slot < buffers.length; slot++) {
            if (buffers[slot] != GraphicsDevice.NO_BUFFER) {
                device.deleteBuffer(buffers[slot]);
                buffers[slot] = GraphicsDevice.NO_BUFFER;
            }
            sizes[slot] = 0;
            scratch[slot] = null;
            uploaded[slot] = 0L;
        }
        needed.clear();
    }
}
