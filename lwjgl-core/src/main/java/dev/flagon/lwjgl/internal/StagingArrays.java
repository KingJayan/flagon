package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.BufferUsage;
import dev.flagon.platform.graphics.GraphicsDevice;

import java.nio.Buffer;

/**
 * points attribute slots at their data, uploading anything that lives in client memory
 */
public final class StagingArrays {

    /** upload every element */
    public static final int WHOLE = -1;

    private final GraphicsDevice device;
    private final int[] buffers;
    private final int[] sizes;
    private final byte[][] bytes;

    public StagingArrays(GraphicsDevice device, int slots) {
        this.device = device;
        this.buffers = new int[slots];
        this.sizes = new int[slots];
        this.bytes = new byte[slots][];
    }

    public void bind(int slot, ClientArray array, int first, int count) {
        if (array.buffer() != GraphicsDevice.NO_BUFFER) {
            device.bindBuffer(BufferTarget.ARRAY, array.buffer());
        } else {
            device.bindBuffer(BufferTarget.ARRAY, buffer(slot));
            upload(slot, array, first, count);
        }
        if (array.isInteger()) {
            device.integerAttributePointer(slot, array.components(), array.type(), array.stride(),
                    array.offset());
        } else {
            device.attributePointer(slot, array.components(), array.type(), array.isNormalized(),
                    array.stride(), array.offset());
        }
    }

    private int buffer(int slot) {
        if (buffers[slot] == GraphicsDevice.NO_BUFFER) {
            buffers[slot] = device.createBuffer();
            sizes[slot] = 0;
        }
        return buffers[slot];
    }

    private void upload(int slot, ClientArray array, int first, int count) {
        Buffer client = array.client();
        int elementSize = Buffers.elementSize(client);
        int size = client.limit() * elementSize;
        if (sizes[slot] != size) {
            device.bufferData(BufferTarget.ARRAY, size, BufferUsage.STREAM);
            sizes[slot] = size;
        }
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
        if (toElement <= fromElement) {
            return;
        }
        byte[] data = scratch(slot, (toElement - fromElement) * elementSize);
        int wrote = Buffers.copy(client, fromElement, toElement, data);
        device.bufferSubData(BufferTarget.ARRAY, fromElement * elementSize, data, wrote);
    }

    private byte[] scratch(int slot, int needed) {
        byte[] data = bytes[slot];
        if (data == null || data.length < needed) {
            data = new byte[needed];
            bytes[slot] = data;
        }
        return data;
    }

    public void dispose() {
        for (int slot = 0; slot < buffers.length; slot++) {
            if (buffers[slot] != GraphicsDevice.NO_BUFFER) {
                device.deleteBuffer(buffers[slot]);
                buffers[slot] = GraphicsDevice.NO_BUFFER;
            }
            sizes[slot] = 0;
            bytes[slot] = null;
        }
    }
}
