package dev.flagon.platform.graphics;

import dev.flagon.platform.UnsupportedCapabilityException;

import java.io.IOException;

/** the host's 2d raster: the canvases java.awt draws on, and the image files it decodes */
public interface Drawing {

    boolean isAvailable();

    /** @return a transparent canvas of this size */
    Canvas2D canvas(int width, int height);

    /**
     * decodes a whole image file, pausing only the calling thread.
     * @throws IOException when the host cannot read the file
     */
    Decoded decode(byte[] file) throws IOException;

    /** @param argb one 0xAARRGGBB int per pixel, row by row, not premultiplied */
    record Decoded(int width, int height, int[] argb) {
    }

    /** @return a drawing that refuses every call with this reason */
    static Drawing unavailable(String reason) {
        return new Drawing() {
            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public Canvas2D canvas(int width, int height) {
                throw new UnsupportedCapabilityException(reason);
            }

            @Override
            public Decoded decode(byte[] file) throws IOException {
                throw new IOException(reason);
            }
        };
    }
}
