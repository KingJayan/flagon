package dev.flagon.platform.graphics;

/** what the device reports went wrong, in the terms gl es uses. */
public enum GraphicsError {
    NONE,
    INVALID_ENUM,
    INVALID_VALUE,
    INVALID_OPERATION,
    INVALID_FRAMEBUFFER_OPERATION,
    OUT_OF_MEMORY,
    CONTEXT_LOST
}
