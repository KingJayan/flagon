package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.AttributeType;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * loc that a vertex attribute reads from
 */
public final class ClientArray {

    private final int defaultComponents;

    private boolean enabled;
    private int components;
    private AttributeType type = AttributeType.FLOAT;
    private boolean normalized;
    private boolean integer;
    private int stride;
    private int buffer;
    private int offset;
    private int divisor;
    private Buffer client;

    ClientArray(int defaultComponents) {
        this.defaultComponents = defaultComponents;
        this.components = defaultComponents;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public boolean isBound() {
        return buffer != 0 || client != null;
    }

    public void pointAt(int buffer, int offset, int components, AttributeType type,
                        boolean normalized, int stride) {
        this.buffer = buffer;
        this.client = null;
        this.offset = offset;
        this.integer = false;
        describe(components, type, normalized, stride);
    }

    /** points at data a shader reads as int or uint, which gl never normalizes or converts. */
    public void pointAtIntegers(int buffer, int offset, int components, AttributeType type,
                                int stride) {
        this.buffer = buffer;
        this.client = null;
        this.offset = offset;
        this.integer = true;
        describe(components, type, false, stride);
    }

    public void pointAt(Buffer client, int components, AttributeType type,
                        boolean normalized, int stride) {
        this.buffer = 0;
        this.client = client;
        this.integer = false;
        this.offset = client.position() * Buffers.elementSize(client);
        describe(components, type, normalized, stride);
    }

    private void describe(int components, AttributeType type, boolean normalized, int stride) {
        this.components = components;
        this.type = type;
        this.normalized = normalized;

        this.stride = stride == 0 ? components * type.sizeInBytes() : stride;
    }

    /** one component of a vertex in client memory, converted to float as gl converts it */
    public float read(int vertex, int component) {
        int at = offset + vertex * stride + component * type.sizeInBytes();
        double value;
        if (client instanceof ByteBuffer bytes) {
            value = switch (type) {
                case BYTE -> bytes.get(at);
                case UNSIGNED_BYTE -> bytes.get(at) & 0xFF;
                case SHORT -> bytes.getShort(at);
                case UNSIGNED_SHORT -> bytes.getShort(at) & 0xFFFF;
                case INT -> bytes.getInt(at);
                case UNSIGNED_INT -> bytes.getInt(at) & 0xFFFFFFFFL;
                case FLOAT -> bytes.getFloat(at);
            };
        } else if (client instanceof FloatBuffer floats) {
            value = floats.get(at / Float.BYTES);
        } else if (client instanceof ShortBuffer shorts) {
            short raw = shorts.get(at / Short.BYTES);
            value = type == AttributeType.UNSIGNED_SHORT ? raw & 0xFFFF : raw;
        } else if (client instanceof IntBuffer ints) {
            int raw = ints.get(at / Integer.BYTES);
            value = type == AttributeType.UNSIGNED_INT ? raw & 0xFFFFFFFFL : raw;
        } else {
            throw new UnsupportedOperationException(
                    "a display list reads vertex arrays from client memory; this array is in a "
                            + "buffer object, so draw it outside glNewList/glEndList");
        }
        if (!normalized || type == AttributeType.FLOAT) {
            return (float) value;
        }
        double largest = switch (type) {
            case BYTE -> Byte.MAX_VALUE;
            case UNSIGNED_BYTE -> 0xFF;
            case SHORT -> Short.MAX_VALUE;
            case UNSIGNED_SHORT -> 0xFFFF;
            case INT -> Integer.MAX_VALUE;
            case UNSIGNED_INT -> 0xFFFFFFFFL;
            case FLOAT -> 1;
        };
        return (float) Math.max(-1, value / largest);
    }

    public int buffer() {
        return buffer;
    }

    public Buffer client() {
        return client;
    }

    public int offset() {
        return offset;
    }

    public int components() {
        return components;
    }

    public AttributeType type() {
        return type;
    }

    public boolean isNormalized() {
        return normalized;
    }

    public boolean isInteger() {
        return integer;
    }

    public int stride() {
        return stride;
    }

    public int divisor() {
        return divisor;
    }

    public void setDivisor(int value) {
        divisor = value;
    }

    void reset() {
        enabled = false;
        buffer = 0;
        client = null;
        offset = 0;
        components = defaultComponents;
        type = AttributeType.FLOAT;
        normalized = false;
        integer = false;
        stride = 0;
        divisor = 0;
    }
}
