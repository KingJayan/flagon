package javax.crypto;

final class Aes {

    private static final int[] SBOX = new int[256];
    private static final int[] INVERSE_SBOX = new int[256];
    private static final int[] ROUND_CONSTANTS = new int[11];

    static {
        buildBoxes();
        int constant = 1;
        for (int round = 1; round <= 10; round++) {
            ROUND_CONSTANTS[round] = constant;
            constant = times(constant, 2);
        }
    }

    private final int[][] roundKeys;
    private final int rounds;

    Aes(byte[] key) {
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalArgumentException(
                    "an aes key is 16, 24 or 32 bytes, got " + key.length);
        }
        int words = key.length / 4;
        this.rounds = words + 6;
        int[] schedule = new int[4 * (rounds + 1)];
        for (int word = 0; word < words; word++) {
            schedule[word] = (key[word * 4] & 0xFF) << 24 | (key[word * 4 + 1] & 0xFF) << 16
                    | (key[word * 4 + 2] & 0xFF) << 8 | (key[word * 4 + 3] & 0xFF);
        }
        for (int word = words; word < schedule.length; word++) {
            int previous = schedule[word - 1];
            if (word % words == 0) {
                previous = substitute(Integer.rotateLeft(previous, 8))
                        ^ (ROUND_CONSTANTS[word / words] << 24);
            } else if (words > 6 && word % words == 4) {
                previous = substitute(previous);
            }
            schedule[word] = schedule[word - words] ^ previous;
        }
        this.roundKeys = new int[rounds + 1][4];
        for (int round = 0; round <= rounds; round++) {
            System.arraycopy(schedule, round * 4, roundKeys[round], 0, 4);
        }
    }

    void encryptBlock(byte[] from, int at, byte[] into, int to) {
        int[] state = load(from, at);
        addKey(state, roundKeys[0]);
        for (int round = 1; round < rounds; round++) {
            substituteBytes(state, SBOX);
            shiftRows(state);
            mixColumns(state);
            addKey(state, roundKeys[round]);
        }
        substituteBytes(state, SBOX);
        shiftRows(state);
        addKey(state, roundKeys[rounds]);
        store(state, into, to);
    }

    void decryptBlock(byte[] from, int at, byte[] into, int to) {
        int[] state = load(from, at);
        addKey(state, roundKeys[rounds]);
        for (int round = rounds - 1; round >= 1; round--) {
            unshiftRows(state);
            substituteBytes(state, INVERSE_SBOX);
            addKey(state, roundKeys[round]);
            unmixColumns(state);
        }
        unshiftRows(state);
        substituteBytes(state, INVERSE_SBOX);
        addKey(state, roundKeys[0]);
        store(state, into, to);
    }

    private static int[] load(byte[] from, int at) {
        int[] state = new int[16];
        for (int index = 0; index < 16; index++) {
            state[(index % 4) * 4 + index / 4] = from[at + index] & 0xFF;
        }
        return state;
    }

    private static void store(int[] state, byte[] into, int to) {
        for (int index = 0; index < 16; index++) {
            into[to + index] = (byte) state[(index % 4) * 4 + index / 4];
        }
    }

    private void addKey(int[] state, int[] key) {
        for (int column = 0; column < 4; column++) {
            int word = key[column];
            state[column] ^= (word >>> 24) & 0xFF;
            state[4 + column] ^= (word >>> 16) & 0xFF;
            state[8 + column] ^= (word >>> 8) & 0xFF;
            state[12 + column] ^= word & 0xFF;
        }
    }

    private static void substituteBytes(int[] state, int[] box) {
        for (int index = 0; index < 16; index++) {
            state[index] = box[state[index]];
        }
    }

    private static void shiftRows(int[] state) {
        for (int row = 1; row < 4; row++) {
            int[] moved = new int[4];
            for (int column = 0; column < 4; column++) {
                moved[column] = state[row * 4 + (column + row) % 4];
            }
            System.arraycopy(moved, 0, state, row * 4, 4);
        }
    }

    private static void unshiftRows(int[] state) {
        for (int row = 1; row < 4; row++) {
            int[] moved = new int[4];
            for (int column = 0; column < 4; column++) {
                moved[(column + row) % 4] = state[row * 4 + column];
            }
            System.arraycopy(moved, 0, state, row * 4, 4);
        }
    }

    private static void mixColumns(int[] state) {
        for (int column = 0; column < 4; column++) {
            int a = state[column];
            int b = state[4 + column];
            int c = state[8 + column];
            int d = state[12 + column];
            state[column] = times(a, 2) ^ times(b, 3) ^ c ^ d;
            state[4 + column] = a ^ times(b, 2) ^ times(c, 3) ^ d;
            state[8 + column] = a ^ b ^ times(c, 2) ^ times(d, 3);
            state[12 + column] = times(a, 3) ^ b ^ c ^ times(d, 2);
        }
    }

    private static void unmixColumns(int[] state) {
        for (int column = 0; column < 4; column++) {
            int a = state[column];
            int b = state[4 + column];
            int c = state[8 + column];
            int d = state[12 + column];
            state[column] = times(a, 14) ^ times(b, 11) ^ times(c, 13) ^ times(d, 9);
            state[4 + column] = times(a, 9) ^ times(b, 14) ^ times(c, 11) ^ times(d, 13);
            state[8 + column] = times(a, 13) ^ times(b, 9) ^ times(c, 14) ^ times(d, 11);
            state[12 + column] = times(a, 11) ^ times(b, 13) ^ times(c, 9) ^ times(d, 14);
        }
    }

    private static int substitute(int word) {
        return SBOX[(word >>> 24) & 0xFF] << 24 | SBOX[(word >>> 16) & 0xFF] << 16
                | SBOX[(word >>> 8) & 0xFF] << 8 | SBOX[word & 0xFF];
    }

    private static int times(int left, int right) {
        int product = 0;
        int a = left;
        int b = right;
        while (b != 0) {
            if ((b & 1) != 0) {
                product ^= a;
            }
            boolean high = (a & 0x80) != 0;
            a = (a << 1) & 0xFF;
            if (high) {
                a ^= 0x1B;
            }
            b >>>= 1;
        }
        return product;
    }

    private static void buildBoxes() {
        int[] inverse = new int[256];
        for (int value = 1; value < 256; value++) {
            for (int guess = 1; guess < 256; guess++) {
                if (times(value, guess) == 1) {
                    inverse[value] = guess;
                    break;
                }
            }
        }
        for (int value = 0; value < 256; value++) {
            int x = inverse[value];
            int mixed = x ^ rotate(x, 1) ^ rotate(x, 2) ^ rotate(x, 3) ^ rotate(x, 4) ^ 0x63;
            SBOX[value] = mixed;
            INVERSE_SBOX[mixed] = value;
        }
    }

    private static int rotate(int value, int by) {
        return ((value << by) | (value >>> (8 - by))) & 0xFF;
    }
}
