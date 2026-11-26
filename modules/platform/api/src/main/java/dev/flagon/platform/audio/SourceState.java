package dev.flagon.platform.audio;

/** where a source is in its lifecycle, which is what callers poll between frames. */
public enum SourceState {

    INITIAL,
    PLAYING,
    PAUSED,
    STOPPED
}
