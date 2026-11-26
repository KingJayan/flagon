package java.security;

/**
 * md5, sha-1 and the sha-2 family
 */
final class Digests {

    private Digests() {
    }

    abstract static class Running {

        final byte[] block;
        long taken;
        int held;

        Running(int blockSize) {
            this.block = new byte[blockSize];
        }

        abstract void compress(byte[] from, int at);

        abstract byte[] finish();

        abstract void restart();

        abstract int length();

        abstract Running copy();

        final void update(byte[] bytes, int at, int count) {
            taken += count;
            int from = at;
            int left = count;
            if (held > 0) {
                int room = Math.min(block.length - held, left);
                System.arraycopy(bytes, from, block, held, room);
                held += room;
                from += room;
                left -= room;
                if (held == block.length) {
                    compress(block, 0);
                    held = 0;
                }
            }
            while (left >= block.length) {
                compress(bytes, from);
                from += block.length;
                left -= block.length;
            }
            if (left > 0) {
                System.arraycopy(bytes, from, block, 0, left);
                held = left;
            }
        }

        /** @param bigEndian whether the length goes in with its highest byte first */
        final void pad(boolean bigEndian) {
            long bits = taken * 8L;
            block[held++] = (byte) 0x80;
            // the length takes the last eighth of a block, of which only 8 bytes are used
            if (held > block.length - block.length / 8) {
                java.util.Arrays.fill(block, held, block.length, (byte) 0);
                compress(block, 0);
                held = 0;
            }
            java.util.Arrays.fill(block, held, block.length - 8, (byte) 0);
            for (int at = 0; at < 8; at++) {
                int shift = bigEndian ? (7 - at) * 8 : at * 8;
                block[block.length - 8 + at] = (byte) (bits >>> shift);
            }
            compress(block, 0);
            held = 0;
        }
    }

    /** sha-1, from FIPS 180-4 */
    static final class Sha1 extends Running {

        private final int[] state = {0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0};
        private final int[] schedule = new int[80];

        Sha1() {
            super(64);
        }

        @Override
        int length() {
            return 20;
        }

        @Override
        void restart() {
            state[0] = 0x67452301;
            state[1] = 0xEFCDAB89;
            state[2] = 0x98BADCFE;
            state[3] = 0x10325476;
            state[4] = 0xC3D2E1F0;
            taken = 0;
            held = 0;
        }

        @Override
        Running copy() {
            Sha1 twin = new Sha1();
            System.arraycopy(state, 0, twin.state, 0, state.length);
            System.arraycopy(block, 0, twin.block, 0, block.length);
            twin.taken = taken;
            twin.held = held;
            return twin;
        }

        @Override
        void compress(byte[] from, int at) {
            for (int word = 0; word < 16; word++) {
                schedule[word] = bigEndianInt(from, at + word * 4);
            }
            for (int word = 16; word < 80; word++) {
                schedule[word] = Integer.rotateLeft(schedule[word - 3] ^ schedule[word - 8]
                        ^ schedule[word - 14] ^ schedule[word - 16], 1);
            }
            int a = state[0];
            int b = state[1];
            int c = state[2];
            int d = state[3];
            int e = state[4];
            for (int round = 0; round < 80; round++) {
                int mixed;
                int constant;
                if (round < 20) {
                    mixed = (b & c) | (~b & d);
                    constant = 0x5A827999;
                } else if (round < 40) {
                    mixed = b ^ c ^ d;
                    constant = 0x6ED9EBA1;
                } else if (round < 60) {
                    mixed = (b & c) | (b & d) | (c & d);
                    constant = 0x8F1BBCDC;
                } else {
                    mixed = b ^ c ^ d;
                    constant = 0xCA62C1D6;
                }
                int next = Integer.rotateLeft(a, 5) + mixed + e + constant + schedule[round];
                e = d;
                d = c;
                c = Integer.rotateLeft(b, 30);
                b = a;
                a = next;
            }
            state[0] += a;
            state[1] += b;
            state[2] += c;
            state[3] += d;
            state[4] += e;
        }

        @Override
        byte[] finish() {
            pad(true);
            byte[] out = new byte[20];
            for (int word = 0; word < 5; word++) {
                putBigEndianInt(out, word * 4, state[word]);
            }
            return out;
        }
    }

    /** sha-256, from FIPS 180-4 */
    static final class Sha256 extends Running {

        private static final int[] ROUND_CONSTANTS = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1,
            0x923f82a4, 0xab1c5ed5, 0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
            0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174, 0xe49b69c1, 0xefbe4786,
            0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147,
            0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
            0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85, 0xa2bfe8a1, 0xa81a664b,
            0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a,
            0x5b9cca4f, 0x682e6ff3, 0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
            0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
        };

        private static final int[] START = {
            0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
            0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19,
        };

        private final int[] state = START.clone();
        private final int[] schedule = new int[64];

        Sha256() {
            super(64);
        }

        @Override
        int length() {
            return 32;
        }

        @Override
        void restart() {
            System.arraycopy(START, 0, state, 0, START.length);
            taken = 0;
            held = 0;
        }

        @Override
        Running copy() {
            Sha256 twin = new Sha256();
            System.arraycopy(state, 0, twin.state, 0, state.length);
            System.arraycopy(block, 0, twin.block, 0, block.length);
            twin.taken = taken;
            twin.held = held;
            return twin;
        }

        @Override
        void compress(byte[] from, int at) {
            for (int word = 0; word < 16; word++) {
                schedule[word] = bigEndianInt(from, at + word * 4);
            }
            for (int word = 16; word < 64; word++) {
                int before = schedule[word - 15];
                int near = schedule[word - 2];
                int low = Integer.rotateRight(before, 7) ^ Integer.rotateRight(before, 18)
                        ^ (before >>> 3);
                int high = Integer.rotateRight(near, 17) ^ Integer.rotateRight(near, 19)
                        ^ (near >>> 10);
                schedule[word] = schedule[word - 16] + low + schedule[word - 7] + high;
            }
            int a = state[0];
            int b = state[1];
            int c = state[2];
            int d = state[3];
            int e = state[4];
            int f = state[5];
            int g = state[6];
            int h = state[7];
            for (int round = 0; round < 64; round++) {
                int spreadE = Integer.rotateRight(e, 6) ^ Integer.rotateRight(e, 11)
                        ^ Integer.rotateRight(e, 25);
                int chosen = (e & f) ^ (~e & g);
                int first = h + spreadE + chosen + ROUND_CONSTANTS[round] + schedule[round];
                int spreadA = Integer.rotateRight(a, 2) ^ Integer.rotateRight(a, 13)
                        ^ Integer.rotateRight(a, 22);
                int agreed = (a & b) ^ (a & c) ^ (b & c);
                int second = spreadA + agreed;
                h = g;
                g = f;
                f = e;
                e = d + first;
                d = c;
                c = b;
                b = a;
                a = first + second;
            }
            state[0] += a;
            state[1] += b;
            state[2] += c;
            state[3] += d;
            state[4] += e;
            state[5] += f;
            state[6] += g;
            state[7] += h;
        }

        @Override
        byte[] finish() {
            pad(true);
            byte[] out = new byte[32];
            for (int word = 0; word < 8; word++) {
                putBigEndianInt(out, word * 4, state[word]);
            }
            return out;
        }
    }

    /** sha-512, and sha-384 cut from it with its own start, from FIPS 180-4 */
    static final class Sha512 extends Running {

        private static final long[] ROUND_CONSTANTS = {
            0x428a2f98d728ae22L, 0x7137449123ef65cdL, 0xb5c0fbcfec4d3b2fL,
            0xe9b5dba58189dbbcL, 0x3956c25bf348b538L, 0x59f111f1b605d019L,
            0x923f82a4af194f9bL, 0xab1c5ed5da6d8118L, 0xd807aa98a3030242L,
            0x12835b0145706fbeL, 0x243185be4ee4b28cL, 0x550c7dc3d5ffb4e2L,
            0x72be5d74f27b896fL, 0x80deb1fe3b1696b1L, 0x9bdc06a725c71235L,
            0xc19bf174cf692694L, 0xe49b69c19ef14ad2L, 0xefbe4786384f25e3L,
            0x0fc19dc68b8cd5b5L, 0x240ca1cc77ac9c65L, 0x2de92c6f592b0275L,
            0x4a7484aa6ea6e483L, 0x5cb0a9dcbd41fbd4L, 0x76f988da831153b5L,
            0x983e5152ee66dfabL, 0xa831c66d2db43210L, 0xb00327c898fb213fL,
            0xbf597fc7beef0ee4L, 0xc6e00bf33da88fc2L, 0xd5a79147930aa725L,
            0x06ca6351e003826fL, 0x142929670a0e6e70L, 0x27b70a8546d22ffcL,
            0x2e1b21385c26c926L, 0x4d2c6dfc5ac42aedL, 0x53380d139d95b3dfL,
            0x650a73548baf63deL, 0x766a0abb3c77b2a8L, 0x81c2c92e47edaee6L,
            0x92722c851482353bL, 0xa2bfe8a14cf10364L, 0xa81a664bbc423001L,
            0xc24b8b70d0f89791L, 0xc76c51a30654be30L, 0xd192e819d6ef5218L,
            0xd69906245565a910L, 0xf40e35855771202aL, 0x106aa07032bbd1b8L,
            0x19a4c116b8d2d0c8L, 0x1e376c085141ab53L, 0x2748774cdf8eeb99L,
            0x34b0bcb5e19b48a8L, 0x391c0cb3c5c95a63L, 0x4ed8aa4ae3418acbL,
            0x5b9cca4f7763e373L, 0x682e6ff3d6b2b8a3L, 0x748f82ee5defb2fcL,
            0x78a5636f43172f60L, 0x84c87814a1f0ab72L, 0x8cc702081a6439ecL,
            0x90befffa23631e28L, 0xa4506cebde82bde9L, 0xbef9a3f7b2c67915L,
            0xc67178f2e372532bL, 0xca273eceea26619cL, 0xd186b8c721c0c207L,
            0xeada7dd6cde0eb1eL, 0xf57d4f7fee6ed178L, 0x06f067aa72176fbaL,
            0x0a637dc5a2c898a6L, 0x113f9804bef90daeL, 0x1b710b35131c471bL,
            0x28db77f523047d84L, 0x32caab7b40c72493L, 0x3c9ebe0a15c9bebcL,
            0x431d67c49c100d4cL, 0x4cc5d4becb3e42b6L, 0x597f299cfc657e2aL,
            0x5fcb6fab3ad6faecL, 0x6c44198c4a475817L,
        };

        private static final long[] START_512 = {
            0x6a09e667f3bcc908L, 0xbb67ae8584caa73bL, 0x3c6ef372fe94f82bL,
            0xa54ff53a5f1d36f1L, 0x510e527fade682d1L, 0x9b05688c2b3e6c1fL,
            0x1f83d9abfb41bd6bL, 0x5be0cd19137e2179L,
        };

        private static final long[] START_384 = {
            0xcbbb9d5dc1059ed8L, 0x629a292a367cd507L, 0x9159015a3070dd17L,
            0x152fecd8f70e5939L, 0x67332667ffc00b31L, 0x8eb44a8768581511L,
            0xdb0c2e0d64f98fa7L, 0x47b5481dbefa4fa4L,
        };

        private final long[] start;
        private final int length;
        private final long[] state;
        private final long[] schedule = new long[80];

        private Sha512(boolean full) {
            super(128);
            this.start = full ? START_512 : START_384;
            this.length = full ? 64 : 48;
            this.state = start.clone();
        }

        static Sha512 full() {
            return new Sha512(true);
        }

        static Sha512 cut() {
            return new Sha512(false);
        }

        @Override
        int length() {
            return length;
        }

        @Override
        void restart() {
            System.arraycopy(start, 0, state, 0, start.length);
            taken = 0;
            held = 0;
        }

        @Override
        Running copy() {
            Sha512 twin = new Sha512(length == 64);
            System.arraycopy(state, 0, twin.state, 0, state.length);
            System.arraycopy(block, 0, twin.block, 0, block.length);
            twin.taken = taken;
            twin.held = held;
            return twin;
        }

        @Override
        void compress(byte[] from, int at) {
            for (int word = 0; word < 16; word++) {
                schedule[word] = (long) bigEndianInt(from, at + word * 8) << 32
                        | (bigEndianInt(from, at + word * 8 + 4) & 0xFFFFFFFFL);
            }
            for (int word = 16; word < 80; word++) {
                long before = schedule[word - 15];
                long near = schedule[word - 2];
                long low = Long.rotateRight(before, 1) ^ Long.rotateRight(before, 8)
                        ^ (before >>> 7);
                long high = Long.rotateRight(near, 19) ^ Long.rotateRight(near, 61)
                        ^ (near >>> 6);
                schedule[word] = schedule[word - 16] + low + schedule[word - 7] + high;
            }
            long a = state[0];
            long b = state[1];
            long c = state[2];
            long d = state[3];
            long e = state[4];
            long f = state[5];
            long g = state[6];
            long h = state[7];
            for (int round = 0; round < 80; round++) {
                long spreadE = Long.rotateRight(e, 14) ^ Long.rotateRight(e, 18)
                        ^ Long.rotateRight(e, 41);
                long chosen = (e & f) ^ (~e & g);
                long first = h + spreadE + chosen + ROUND_CONSTANTS[round] + schedule[round];
                long spreadA = Long.rotateRight(a, 28) ^ Long.rotateRight(a, 34)
                        ^ Long.rotateRight(a, 39);
                long agreed = (a & b) ^ (a & c) ^ (b & c);
                long second = spreadA + agreed;
                h = g;
                g = f;
                f = e;
                e = d + first;
                d = c;
                c = b;
                b = a;
                a = first + second;
            }
            state[0] += a;
            state[1] += b;
            state[2] += c;
            state[3] += d;
            state[4] += e;
            state[5] += f;
            state[6] += g;
            state[7] += h;
        }

        @Override
        byte[] finish() {
            pad(true);
            byte[] out = new byte[length];
            for (int word = 0; word < length / 8; word++) {
                putBigEndianInt(out, word * 8, (int) (state[word] >>> 32));
                putBigEndianInt(out, word * 8 + 4, (int) state[word]);
            }
            return out;
        }
    }

    static final class Md5 extends Running {

        private static final int[] SHIFTS = {
            7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
            5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
            4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
            6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
        };

        private static final int[] SINES = new int[64];

        static {
            for (int round = 0; round < 64; round++) {
                SINES[round] = (int) (long) (Math.abs(Math.sin(round + 1.0)) * 4294967296.0);
            }
        }

        private final int[] state = {0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476};
        private final int[] words = new int[16];

        Md5() {
            super(64);
        }

        @Override
        int length() {
            return 16;
        }

        @Override
        void restart() {
            state[0] = 0x67452301;
            state[1] = 0xefcdab89;
            state[2] = 0x98badcfe;
            state[3] = 0x10325476;
            taken = 0;
            held = 0;
        }

        @Override
        Running copy() {
            Md5 twin = new Md5();
            System.arraycopy(state, 0, twin.state, 0, state.length);
            System.arraycopy(block, 0, twin.block, 0, block.length);
            twin.taken = taken;
            twin.held = held;
            return twin;
        }

        @Override
        void compress(byte[] from, int at) {
            for (int word = 0; word < 16; word++) {
                words[word] = littleEndianInt(from, at + word * 4);
            }
            int a = state[0];
            int b = state[1];
            int c = state[2];
            int d = state[3];
            for (int round = 0; round < 64; round++) {
                int mixed;
                int pick;
                if (round < 16) {
                    mixed = (b & c) | (~b & d);
                    pick = round;
                } else if (round < 32) {
                    mixed = (d & b) | (~d & c);
                    pick = (5 * round + 1) % 16;
                } else if (round < 48) {
                    mixed = b ^ c ^ d;
                    pick = (3 * round + 5) % 16;
                } else {
                    mixed = c ^ (b | ~d);
                    pick = (7 * round) % 16;
                }
                int turned = a + mixed + SINES[round] + words[pick];
                a = d;
                d = c;
                c = b;
                b = b + Integer.rotateLeft(turned, SHIFTS[round]);
            }
            state[0] += a;
            state[1] += b;
            state[2] += c;
            state[3] += d;
        }

        @Override
        byte[] finish() {
            pad(false);
            byte[] out = new byte[16];
            for (int word = 0; word < 4; word++) {
                putLittleEndianInt(out, word * 4, state[word]);
            }
            return out;
        }
    }

    static int bigEndianInt(byte[] bytes, int at) {
        return (bytes[at] & 0xFF) << 24 | (bytes[at + 1] & 0xFF) << 16
                | (bytes[at + 2] & 0xFF) << 8 | (bytes[at + 3] & 0xFF);
    }

    static void putBigEndianInt(byte[] bytes, int at, int value) {
        bytes[at] = (byte) (value >>> 24);
        bytes[at + 1] = (byte) (value >>> 16);
        bytes[at + 2] = (byte) (value >>> 8);
        bytes[at + 3] = (byte) value;
    }

    static int littleEndianInt(byte[] bytes, int at) {
        return (bytes[at] & 0xFF) | (bytes[at + 1] & 0xFF) << 8
                | (bytes[at + 2] & 0xFF) << 16 | (bytes[at + 3] & 0xFF) << 24;
    }

    static void putLittleEndianInt(byte[] bytes, int at, int value) {
        bytes[at] = (byte) value;
        bytes[at + 1] = (byte) (value >>> 8);
        bytes[at + 2] = (byte) (value >>> 16);
        bytes[at + 3] = (byte) (value >>> 24);
    }
}
