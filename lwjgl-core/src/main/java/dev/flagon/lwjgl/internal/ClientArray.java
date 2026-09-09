package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.AttributeType;

import java.nio.Buffer;

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
