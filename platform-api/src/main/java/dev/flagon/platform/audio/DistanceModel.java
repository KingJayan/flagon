package dev.flagon.platform.audio;

/**
 how a source's gain falls off with distance, which decides whether its rolloff is applied at all
 */
public enum DistanceModel {

    /** distance is ignored; a source is as loud far away as it is near */
    NONE,

    INVERSE,

    /** inverse, with distance held between the reference and maximum distances */
    INVERSE_CLAMPED
}
