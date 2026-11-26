package dev.flagon.platform.graphics;

/** whether a framebuffer can be drawn to, as glCheckFramebufferStatus reports it. */
public enum FramebufferStatus {
    COMPLETE,
    INCOMPLETE_ATTACHMENT,
    INCOMPLETE_MISSING_ATTACHMENT,
    INCOMPLETE_DIMENSIONS,
    UNSUPPORTED,
    UNKNOWN
}
