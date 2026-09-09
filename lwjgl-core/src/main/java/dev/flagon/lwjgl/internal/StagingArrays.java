package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.BufferUsage;
import dev.flagon.platform.graphics.GraphicsDevice;

/**
 * points attribute slots at their data, uploading anything that lives in client memory
 */
public final class StagingArrays {

    private final GraphicsDevice device;
    private final int[] buffers;
    private final byte[][] bytes;

    public StagingArrays(GraphicsDevice device, int slots) {
        this.device = device;
        this.buffers = new int[slots];
        this.bytes = new byte[slots][];
    }

    public void bind(int slot, ClientArray array) {
        if (array.buffer() != GraphicsDevice.NO_BUFFER) {
            device.bindBuffer(BufferTarget.ARRAY, array.buffer());
        } else {
            device.bindBuffer(BufferTarget.ARRAY, buffer(slot));
            byte[] data = staged(slot, array);
            device.bufferData(BufferTarget.ARRAY, data, data.length, BufferUsage.STREAM);
        }
        device.attributePointer(slot, array.components(), array.type(), array.isNormalized(),
                array.stride(), array.offset());
    }

    private int buffer(int slot) {
        if (buffers[slot] == GraphicsDevice.NO_BUFFER) {
            buffers[slot] = device.createBuffer();
        }
        return buffers[slot];
    }

    private byte[] staged(int slot, ClientArray array) {
        int needed = array.client().capacity() * Buffers.elementSize(array.client());
        byte[] data = bytes[slot];
        if (data == null || data.length != needed) {
            data = new byte[needed];
            bytes[slot] = data;
        }
        Buffers.readAll(array.client(), data);
        return data;
    }

    public void dispose() {
        for (int slot = 0; slot < buffers.length; slot++) {
            if (buffers[slot] != GraphicsDevice.NO_BUFFER) {
                device.deleteBuffer(buffers[slot]);
                buffers[slot] = GraphicsDevice.NO_BUFFER;
            }
            bytes[slot] = null;
        }
    }
}
