package java.io;

/** SHA-1, which the default serialVersionUID of an array class is made from */
final class Sha1 {

    private Sha1() {
    }

    static byte[] digest(byte[] message) {
        int[] h = {0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0};
        long bits = (long) message.length * 8;
        int padded = ((message.length + 8) / 64 + 1) * 64;
        byte[] data = new byte[padded];
        System.arraycopy(message, 0, data, 0, message.length);
        data[message.length] = (byte) 0x80;
        for (int at = 0; at < 8; at++) {
            data[padded - 1 - at] = (byte) (bits >>> (8 * at));
        }
        int[] w = new int[80];
        for (int block = 0; block < padded; block += 64) {
            for (int at = 0; at < 16; at++) {
                int base = block + at * 4;
                w[at] = (data[base] & 0xFF) << 24 | (data[base + 1] & 0xFF) << 16
                        | (data[base + 2] & 0xFF) << 8 | data[base + 3] & 0xFF;
            }
            for (int at = 16; at < 80; at++) {
                w[at] = Integer.rotateLeft(w[at - 3] ^ w[at - 8] ^ w[at - 14] ^ w[at - 16], 1);
            }
            int a = h[0];
            int b = h[1];
            int c = h[2];
            int d = h[3];
            int e = h[4];
            for (int at = 0; at < 80; at++) {
                int f;
                int k;
                if (at < 20) {
                    f = (b & c) | (~b & d);
                    k = 0x5A827999;
                } else if (at < 40) {
                    f = b ^ c ^ d;
                    k = 0x6ED9EBA1;
                } else if (at < 60) {
                    f = (b & c) | (b & d) | (c & d);
                    k = 0x8F1BBCDC;
                } else {
                    f = b ^ c ^ d;
                    k = 0xCA62C1D6;
                }
                int next = Integer.rotateLeft(a, 5) + f + e + k + w[at];
                e = d;
                d = c;
                c = Integer.rotateLeft(b, 30);
                b = a;
                a = next;
            }
            h[0] += a;
            h[1] += b;
            h[2] += c;
            h[3] += d;
            h[4] += e;
        }
        byte[] out = new byte[20];
        for (int at = 0; at < 20; at++) {
            out[at] = (byte) (h[at / 4] >>> (24 - 8 * (at % 4)));
        }
        return out;
    }
}
