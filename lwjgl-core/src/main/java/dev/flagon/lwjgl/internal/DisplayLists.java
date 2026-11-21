package dev.flagon.lwjgl.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * gl display lists as flat arrays
 */
public final class DisplayLists {

    public static final int COMPILE = 0x1300;
    public static final int COMPILE_AND_EXECUTE = 0x1301;

    /** runs one recorded call. its ints start at {@code at}, and its floats at {@code from} */
    public interface Player {
        void play(int op, int[] codes, int at, float[] values, int from);
    }

    private static final int[] NO_CODES = {};
    private static final float[] NO_VALUES = {};

    /** what a list name holds. one record is op, int count, float count, then its ints */
    private static final class Held {

        private final int[] codes;
        private final float[] values;
        private int[] blocks;

        Held(int[] codes, float[] values, int[] blocks) {
            this.codes = codes;
            this.values = values;
            this.blocks = blocks;
        }
    }

    // a list name is its index
    private final List<Held> lists = new ArrayList<>();
    private IntConsumer release = block -> { };
    private Player player = (op, codes, at, values, from) -> { };

    private int[] codes = NO_CODES;
    private int codeCount;
    private float[] values = NO_VALUES;
    private int valueCount;
    private int[] blocks = NO_CODES;
    private int blockCount;
    private boolean recording;
    private int recordingInto;
    private boolean executeWhileRecording;
    private int nextName = 1;
    private static final int FAR = 1 << 16;
    private final Map<Integer, Held> far = new HashMap<>();

    /** a list owns the vertices its blocks uploaded, so they go when the list does */
    public void onRelease(IntConsumer release) {
        this.release = release;
    }

    public void onPlay(Player player) {
        this.player = player;
    }

    public void own(int block) {
        if (recording) {
            blocks = grow(blocks, blockCount, 1);
            blocks[blockCount++] = block;
        }
    }

    public int generate(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("glGenLists: negative count " + count);
        }
        if (count == 0) {
            return 0;
        }
        int first = nextName;
        nextName += count;
        for (int name = first; name < nextName; name++) {
            put(name, new Held(NO_CODES, NO_VALUES, null));
        }
        return first;
    }

    public boolean exists(int name) {
        return held(name) != null;
    }

    public void begin(int name, int mode) {
        if (recording) {
            throw new IllegalStateException("glNewList called inside glNewList/glEndList");
        }
        if (name == 0) {
            throw new IllegalArgumentException("glNewList: list names start at 1, got 0");
        }
        if (mode != COMPILE && mode != COMPILE_AND_EXECUTE) {
            throw new IllegalArgumentException(
                    "glNewList: unknown mode 0x" + Integer.toHexString(mode));
        }
        codeCount = 0;
        valueCount = 0;
        blockCount = 0;
        recording = true;
        recordingInto = name;
        executeWhileRecording = mode == COMPILE_AND_EXECUTE;
        if (dense(name)) {
            nextName = Math.max(nextName, name + 1);
        }
    }

    public void end() {
        if (!recording) {
            throw new IllegalStateException("glEndList called without glNewList");
        }
        free(recordingInto);
        put(recordingInto, new Held(Arrays.copyOf(codes, codeCount),
                Arrays.copyOf(values, valueCount), Arrays.copyOf(blocks, blockCount)));
        recording = false;
        executeWhileRecording = false;
    }

    public boolean isRecording() {
        return recording;
    }

    public boolean record(int op) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 0, 0);
        return held;
    }

    public boolean record(int op, int first) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 1, 0);
        codes[codeCount++] = first;
        return held;
    }

    public boolean record(int op, int first, int second) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 2, 0);
        codes[codeCount++] = first;
        codes[codeCount++] = second;
        return held;
    }

    public boolean record(int op, int first, int second, int third) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 3, 0);
        codes[codeCount++] = first;
        codes[codeCount++] = second;
        codes[codeCount++] = third;
        return held;
    }

    public boolean record(int op, int first, int second, int third, int fourth) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 4, 0);
        codes[codeCount++] = first;
        codes[codeCount++] = second;
        codes[codeCount++] = third;
        codes[codeCount++] = fourth;
        return held;
    }

    public boolean record(int op, float a) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 0, 1);
        values[valueCount++] = a;
        return held;
    }

    public boolean record(int op, int first, float a) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 1, 1);
        codes[codeCount++] = first;
        values[valueCount++] = a;
        return held;
    }

    public boolean record(int op, int first, int second, float a) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 2, 1);
        codes[codeCount++] = first;
        codes[codeCount++] = second;
        values[valueCount++] = a;
        return held;
    }

    public boolean record(int op, float a, float b) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 0, 2);
        values[valueCount++] = a;
        values[valueCount++] = b;
        return held;
    }

    public boolean record(int op, int first, float a, float b) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 1, 2);
        codes[codeCount++] = first;
        values[valueCount++] = a;
        values[valueCount++] = b;
        return held;
    }

    public boolean record(int op, float a, float b, float c) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 0, 3);
        values[valueCount++] = a;
        values[valueCount++] = b;
        values[valueCount++] = c;
        return held;
    }

    public boolean record(int op, float a, float b, float c, float d) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 0, 4);
        values[valueCount++] = a;
        values[valueCount++] = b;
        values[valueCount++] = c;
        values[valueCount++] = d;
        return held;
    }

    public boolean record(int op, int first, float a, float b, float c, float d) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 1, 4);
        codes[codeCount++] = first;
        values[valueCount++] = a;
        values[valueCount++] = b;
        values[valueCount++] = c;
        values[valueCount++] = d;
        return held;
    }

    public boolean record(int op, int first, int second, float a, float b, float c, float d) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 2, 4);
        codes[codeCount++] = first;
        codes[codeCount++] = second;
        values[valueCount++] = a;
        values[valueCount++] = b;
        values[valueCount++] = c;
        values[valueCount++] = d;
        return held;
    }

    public boolean record(int op, float a, float b, float c, float d, float e, float g) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 0, 6);
        values[valueCount++] = a;
        values[valueCount++] = b;
        values[valueCount++] = c;
        values[valueCount++] = d;
        values[valueCount++] = e;
        values[valueCount++] = g;
        return held;
    }

    public boolean record(int op, float[] many) {
        if (!recording) {
            return false;
        }
        boolean held = open(op, 0, many.length);
        System.arraycopy(many, 0, values, valueCount, many.length);
        valueCount += many.length;
        return held;
    }

    private boolean open(int op, int ints, int floats) {
        codes = grow(codes, codeCount, 3 + ints);
        codes[codeCount++] = op;
        codes[codeCount++] = ints;
        codes[codeCount++] = floats;
        values = grow(values, valueCount, floats);
        return !executeWhileRecording;
    }

    public void call(int name) {
        Held found = held(name);
        if (found == null) {
            return;
        }
        int[] code = found.codes;
        float[] value = found.values;
        int at = 0;
        int from = 0;
        while (at < code.length) {
            int op = code[at];
            int ints = code[at + 1];
            int floats = code[at + 2];
            player.play(op, code, at + 3, value, from);
            at += 3 + ints;
            from += floats;
        }
    }

    public void delete(int first, int count) {
        for (int at = 0; at < count; at++) {
            int name = first + at;
            free(name);
            if (name > 0 && name < lists.size()) {
                lists.set(name, null);
            } else {
                far.remove(name);
            }
        }
    }

    // gl names are unsigned, so a negative int is a name past 2^31; those, and names far past
    // the ones handed out, are kept apart from the dense run glGenLists fills
    private boolean dense(int name) {
        return name > 0 && name < lists.size() + FAR;
    }

    private Held held(int name) {
        return name > 0 && name < lists.size() ? lists.get(name) : far.get(name);
    }

    private void put(int name, Held into) {
        if (!dense(name)) {
            far.put(name, into);
            return;
        }
        while (lists.size() <= name) {
            lists.add(null);
        }
        lists.set(name, into);
    }

    private void free(int name) {
        Held found = held(name);
        if (found == null || found.blocks == null) {
            return;
        }
        for (int block : found.blocks) {
            release.accept(block);
        }
        found.blocks = null;
    }

    private static int[] grow(int[] held, int used, int more) {
        return used + more <= held.length
                ? held
                : Arrays.copyOf(held, Math.max(16, Math.max(used + more, held.length * 2)));
    }

    private static float[] grow(float[] held, int used, int more) {
        return used + more <= held.length
                ? held
                : Arrays.copyOf(held, Math.max(16, Math.max(used + more, held.length * 2)));
    }
}
