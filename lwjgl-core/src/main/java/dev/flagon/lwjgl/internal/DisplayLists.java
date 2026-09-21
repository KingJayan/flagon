package dev.flagon.lwjgl.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * gl display lists as commands
 */
public final class DisplayLists {

    public static final int COMPILE = 0x1300;
    public static final int COMPILE_AND_EXECUTE = 0x1301;

    private final Map<Integer, List<Runnable>> lists = new HashMap<>();
    private final Map<Integer, List<Integer>> blocks = new HashMap<>();
    private IntConsumer release = block -> { };
    private List<Runnable> recording;
    private List<Integer> recordingBlocks;
    private int recordingInto;
    private boolean executeWhileRecording;
    private int nextName = 1;

    /** a list owns the vertices its blocks uploaded, so they go when the list does */
    public void onRelease(IntConsumer release) {
        this.release = release;
    }

    public void own(int block) {
        if (recordingBlocks != null) {
            recordingBlocks.add(block);
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
            lists.put(name, new ArrayList<>());
        }
        return first;
    }

    public boolean exists(int name) {
        return lists.containsKey(name);
    }

    public void begin(int name, int mode) {
        if (recording != null) {
            throw new IllegalStateException("glNewList called inside glNewList/glEndList");
        }
        if (name <= 0) {
            throw new IllegalArgumentException("glNewList: list names start at 1, got " + name);
        }
        if (mode != COMPILE && mode != COMPILE_AND_EXECUTE) {
            throw new IllegalArgumentException(
                    "glNewList: unknown mode 0x" + Integer.toHexString(mode));
        }
        recording = new ArrayList<>();
        recordingBlocks = new ArrayList<>();
        recordingInto = name;
        executeWhileRecording = mode == COMPILE_AND_EXECUTE;
        nextName = Math.max(nextName, name + 1);
    }

    public void end() {
        if (recording == null) {
            throw new IllegalStateException("glEndList called without glNewList");
        }
        lists.put(recordingInto, recording);
        free(recordingInto);
        blocks.put(recordingInto, recordingBlocks);
        recording = null;
        recordingBlocks = null;
        executeWhileRecording = false;
    }

    public boolean isRecording() {
        return recording != null;
    }

    public boolean record(Runnable call) {
        if (recording == null) {
            return false;
        }
        recording.add(call);
        return !executeWhileRecording;
    }

    public void call(int name) {
        List<Runnable> list = lists.get(name);
        if (list == null) {
            return;
        }
        for (Runnable command : list) {
            command.run();
        }
    }

    public void delete(int first, int count) {
        for (int name = first; name < first + count; name++) {
            lists.remove(name);
            free(name);
        }
    }

    private void free(int name) {
        List<Integer> held = blocks.remove(name);
        if (held != null) {
            for (int block : held) {
                release.accept(block);
            }
        }
    }
}
