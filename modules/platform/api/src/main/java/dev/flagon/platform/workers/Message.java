package dev.flagon.platform.workers;

/**
 * what one end of a {@link Channel} sends the other: text, bytes or both
 * @param text null for none
 * @param data null for none; sent as a copy
 */
public record Message(String text, byte[] data) {
}
