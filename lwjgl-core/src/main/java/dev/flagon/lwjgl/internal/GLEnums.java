package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.AttributeType;
import dev.flagon.platform.graphics.IndexType;

/** the gl enum numbers both bindings share, and what they mean to the layer below. */
public final class GLEnums {

    public static final int GL_POINTS = 0x0000;
    public static final int GL_LINES = 0x0001;
    public static final int GL_LINE_LOOP = 0x0002;
    public static final int GL_LINE_STRIP = 0x0003;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_TRIANGLE_STRIP = 0x0005;
    public static final int GL_TRIANGLE_FAN = 0x0006;

    public static final int GL_BYTE = 0x1400;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_SHORT = 0x1402;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_INT = 0x1404;
    public static final int GL_UNSIGNED_INT = 0x1405;
    public static final int GL_FLOAT = 0x1406;

    private GLEnums() {
    }

    public static DrawMode drawMode(int mode, String call) {
        return switch (mode) {
            case GL_POINTS -> DrawMode.POINTS;
            case GL_LINES -> DrawMode.LINES;
            case GL_LINE_LOOP -> DrawMode.LINE_LOOP;
            case GL_LINE_STRIP -> DrawMode.LINE_STRIP;
            case GL_TRIANGLES -> DrawMode.TRIANGLES;
            case GL_TRIANGLE_STRIP -> DrawMode.TRIANGLE_STRIP;
            case GL_TRIANGLE_FAN -> DrawMode.TRIANGLE_FAN;
            default -> throw new UnsupportedOperationException(
                    call + " does not emulate primitive 0x" + Integer.toHexString(mode));
        };
    }

    public static IndexType indexType(int type, String call) {
        return switch (type) {
            case GL_UNSIGNED_BYTE -> IndexType.UNSIGNED_BYTE;
            case GL_UNSIGNED_SHORT -> IndexType.UNSIGNED_SHORT;
            case GL_UNSIGNED_INT -> IndexType.UNSIGNED_INT;
            default -> throw new IllegalArgumentException(
                    call + ": indices must be unsigned, got 0x" + Integer.toHexString(type));
        };
    }

    public static AttributeType integerAttributeType(int type, String call) {
        return switch (type) {
            case GL_BYTE -> AttributeType.BYTE;
            case GL_UNSIGNED_BYTE -> AttributeType.UNSIGNED_BYTE;
            case GL_SHORT -> AttributeType.SHORT;
            case GL_UNSIGNED_SHORT -> AttributeType.UNSIGNED_SHORT;
            case GL_INT -> AttributeType.INT;
            case GL_UNSIGNED_INT -> AttributeType.UNSIGNED_INT;
            default -> throw new IllegalArgumentException(
                    call + " takes an integer component type, got 0x" + Integer.toHexString(type));
        };
    }
}
