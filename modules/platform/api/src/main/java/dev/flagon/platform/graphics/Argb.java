package dev.flagon.platform.graphics;

/** lays 0xAARRGGBB ints out as r, g, b, a bytes */
public final class Argb {

    private static byte[] scratch = new byte[0];

    private Argb() {
    }

    public static byte[] spread(int[] argb, int count) {
        if (argb.length < count) {
            throw new IllegalArgumentException("need " + count + " pixels, got " + argb.length);
        }
        if (scratch.length < count * 4) {
            scratch = new byte[count * 4];
        }
        byte[] rgba = scratch;
        for (int pixel = 0, out = 0; pixel < count; pixel++) {
            int one = argb[pixel];
            rgba[out++] = (byte) (one >> 16);
            rgba[out++] = (byte) (one >> 8);
            rgba[out++] = (byte) one;
            rgba[out++] = (byte) (one >>> 24);
        }
        return rgba;
    }
}
